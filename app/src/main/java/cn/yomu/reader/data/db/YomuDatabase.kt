package cn.yomu.reader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MountEntity::class,
        AlbumEntity::class,
        AlbumImageEntity::class,
        BookshelfEntity::class,
        AlbumBookshelfEntity::class,
    ],
    version = 3,
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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `album_images` (
                        `albumId` TEXT NOT NULL,
                        `uri` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        PRIMARY KEY(`albumId`, `uri`),
                        FOREIGN KEY(`albumId`) REFERENCES `albums`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_images_albumId` ON `album_images` (`albumId`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_album_images_albumId_position` " +
                        "ON `album_images` (`albumId`, `position`)",
                )
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `mount_sources` ADD COLUMN `path` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `mount_sources` ADD COLUMN `lastSuccessfulRefreshAt` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `mount_sources` ADD COLUMN `lastRefreshFailed` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `albums` ADD COLUMN `customName` TEXT DEFAULT NULL")
            }
        }
    }
}
