package cn.yomu.reader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MountEntity::class, AlbumEntity::class, BookshelfEntity::class, AlbumBookshelfEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class YomuDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile private var instance: YomuDatabase? = null

        fun get(context: Context): YomuDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                YomuDatabase::class.java,
                "yomu-library.db",
            ).build().also { instance = it }
        }
    }
}
