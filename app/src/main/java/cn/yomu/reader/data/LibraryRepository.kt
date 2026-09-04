package cn.yomu.reader.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import cn.yomu.reader.model.Album
import cn.yomu.reader.model.ImageRef
import cn.yomu.reader.model.LibrarySnapshot
import cn.yomu.reader.model.MountedFolder
import cn.yomu.reader.model.ReaderPreferences
import cn.yomu.reader.model.ReadingDirection
import cn.yomu.reader.model.ReadingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class LibraryRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences("yomu_library", Context.MODE_PRIVATE)

    fun persistFolderPermission(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    fun addMount(uri: Uri) {
        val mounts = savedMountUris().toMutableList()
        val value = uri.toString()
        if (value !in mounts) {
            mounts += value
            saveMountUris(mounts)
        }
    }

    fun removeMount(uri: String) {
        saveMountUris(savedMountUris().filterNot { it == uri })
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    suspend fun scanLibrary(): LibrarySnapshot = withContext(Dispatchers.IO) {
        val issues = mutableListOf<String>()
        val mounts = savedMountUris().map { value ->
            val uri = Uri.parse(value)
            val root = runCatching { DocumentFile.fromTreeUri(context, uri) }.getOrNull()
            if (root == null || !root.exists() || !root.isDirectory) {
                issues += "无法读取已挂载目录：$value"
                MountedFolder(value, uri.lastPathSegment?.substringAfterLast(':') ?: "失效目录")
            } else {
                MountedFolder(value, root.name?.takeIf(String::isNotBlank) ?: "未命名目录")
            }
        }

        val albums = buildList {
            mounts.forEach { mount ->
                val root = DocumentFile.fromTreeUri(context, Uri.parse(mount.uri)) ?: return@forEach
                if (!root.exists() || !root.isDirectory) return@forEach
                runCatching { scanDirectory(root, mount, "", this, issues) }
                    .onFailure { issues += "${mount.name} 扫描失败：${it.message ?: "无权访问"}" }
            }
        }.sortedWith(compareBy(NaturalOrder) { it.path })

        LibrarySnapshot(mounts, albums, issues)
    }

    private fun scanDirectory(
        directory: DocumentFile,
        mount: MountedFolder,
        relativePath: String,
        output: MutableList<Album>,
        issues: MutableList<String>,
    ) {
        val children = directory.listFiles().toList()
        val images = children
            .asSequence()
            .filter { it.isFile && it.isReadableImage() }
            .sortedWith(compareBy(NaturalOrder) { it.name.orEmpty() })
            .map { ImageRef(it.uri.toString(), it.name.orEmpty()) }
            .toList()

        if (images.isNotEmpty()) {
            val path = if (relativePath.isBlank()) mount.name else "${mount.name}/$relativePath"
            val albumId = stableId("${mount.uri}|${directory.uri}")
            output += Album(
                id = albumId,
                mountUri = mount.uri,
                name = directory.name?.takeIf(String::isNotBlank) ?: mount.name,
                path = path,
                images = images,
                progress = progressFor(albumId).coerceIn(-1, images.lastIndex),
            )
        }

        children
            .asSequence()
            .filter { it.isDirectory }
            .sortedWith(compareBy(NaturalOrder) { it.name.orEmpty() })
            .forEach { child ->
                val childName = child.name?.takeIf(String::isNotBlank) ?: "未命名目录"
                val childPath = if (relativePath.isBlank()) childName else "$relativePath/$childName"
                // Dot-prefixed folders are intentionally included.
                runCatching { scanDirectory(child, mount, childPath, output, issues) }
                    .onFailure { issues += "$childPath 无法读取，已跳过" }
            }
    }

    private fun DocumentFile.isReadableImage(): Boolean {
        val mime = type.orEmpty().lowercase()
        if (mime.startsWith("image/")) return true
        return name.orEmpty().substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS
    }

    fun saveProgress(albumId: String, page: Int) {
        preferences.edit().putInt("progress_$albumId", page.coerceAtLeast(0)).apply()
    }

    fun progressFor(albumId: String): Int = preferences.getInt("progress_$albumId", -1)

    fun readerPreferences(): ReaderPreferences = ReaderPreferences(
        mode = runCatching {
            ReadingMode.valueOf(preferences.getString("reading_mode", null).orEmpty())
        }.getOrDefault(ReadingMode.PAGER),
        direction = runCatching {
            ReadingDirection.valueOf(preferences.getString("reading_direction", null).orEmpty())
        }.getOrDefault(ReadingDirection.LEFT_TO_RIGHT),
    )

    fun saveReaderPreferences(value: ReaderPreferences) {
        preferences.edit()
            .putString("reading_mode", value.mode.name)
            .putString("reading_direction", value.direction.name)
            .apply()
    }

    private fun savedMountUris(): List<String> = preferences
        .getString("mounted_folders", "")
        .orEmpty()
        .lineSequence()
        .filter(String::isNotBlank)
        .distinct()
        .toList()

    private fun saveMountUris(values: List<String>) {
        preferences.edit().putString("mounted_folders", values.joinToString("\n")).apply()
    }

    private fun stableId(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .take(12)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private companion object {
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif")
    }
}
