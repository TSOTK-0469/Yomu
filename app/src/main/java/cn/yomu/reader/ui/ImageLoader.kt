package cn.yomu.reader.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.sqrt

sealed interface BitmapLoadState {
    data object Loading : BitmapLoadState
    data class Ready(val bitmap: Bitmap) : BitmapLoadState
    data object Failed : BitmapLoadState
}

object LocalImageLoader {
    private val cache = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    suspend fun load(contentResolver: ContentResolver, uri: String, targetWidth: Int): Bitmap? {
        val safeWidth = targetWidth.coerceIn(240, 4096)
        val key = "$uri@$safeWidth"
        cache.get(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            runCatching {
                val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    decodeModern(contentResolver, Uri.parse(uri), safeWidth)
                } else {
                    decodeLegacy(contentResolver, Uri.parse(uri), safeWidth)
                }
                decoded?.also { cache.put(key, it) }
            }.getOrNull()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeModern(resolver: ContentResolver, uri: Uri, targetWidth: Int): Bitmap? {
        val source = ImageDecoder.createSource(resolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val sourceWidth = info.size.width.coerceAtLeast(1)
            val sourceHeight = info.size.height.coerceAtLeast(1)
            if (sourceWidth > targetWidth || sourceWidth.toLong() * sourceHeight > MAX_DECODE_PIXELS) {
                val widthScale = targetWidth.toDouble() / sourceWidth
                val pixelScale = sqrt(MAX_DECODE_PIXELS.toDouble() / (sourceWidth.toLong() * sourceHeight))
                val scale = minOf(1.0, widthScale, pixelScale)
                decoder.setTargetSize(
                    (sourceWidth * scale).toInt().coerceAtLeast(1),
                    (sourceHeight * scale).toInt().coerceAtLeast(1),
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
        }
    }

    private fun decodeLegacy(resolver: ContentResolver, uri: Uri, targetWidth: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (
            bounds.outWidth / (sample * 2) >= targetWidth ||
            bounds.outWidth.toLong() * bounds.outHeight / (sample * 2L * sample * 2L) > MAX_DECODE_PIXELS
        ) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = max(1, sample)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        return rotateForExif(bitmap, orientation)
    }

    private fun rotateForExif(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    }

    private const val MAX_DECODE_PIXELS = 16_000_000L
}

@Composable
fun rememberBitmap(
    resolver: ContentResolver,
    uri: String,
    targetWidth: Int,
): State<BitmapLoadState> = produceState<BitmapLoadState>(
    initialValue = BitmapLoadState.Loading,
    uri,
    targetWidth,
) {
    value = LocalImageLoader.load(resolver, uri, targetWidth)
        ?.let(BitmapLoadState::Ready)
        ?: BitmapLoadState.Failed
}
