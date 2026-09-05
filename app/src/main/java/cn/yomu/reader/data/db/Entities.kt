package cn.yomu.reader.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "mount_sources")
data class MountEntity(
    @PrimaryKey val id: String,
    val treeUri: String,
    val directoryUri: String,
    val name: String,
    val path: String,
    val mode: String,
    val available: Boolean,
    val createdAt: Long,
    val lastSuccessfulRefreshAt: Long?,
    val lastRefreshFailed: Boolean,
)

@Entity(
    tableName = "albums",
    foreignKeys = [ForeignKey(
        entity = MountEntity::class,
        parentColumns = ["id"],
        childColumns = ["mountId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("mountId"), Index("directoryUri")],
)
data class AlbumEntity(
    @PrimaryKey val id: String,
    val mountId: String,
    val directoryUri: String,
    val name: String,
    val customName: String?,
    val path: String,
    val defaultCoverUri: String,
    val customCoverUri: String?,
    val pageCount: Int,
    val lastReadUri: String?,
    val lastReadIndex: Int,
    val hidden: Boolean,
    val updatedAt: Long,
)

@Entity(
    tableName = "album_images",
    primaryKeys = ["albumId", "uri"],
    foreignKeys = [ForeignKey(
        entity = AlbumEntity::class,
        parentColumns = ["id"],
        childColumns = ["albumId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("albumId"), Index(value = ["albumId", "position"], unique = true)],
)
data class AlbumImageEntity(
    val albumId: String,
    val uri: String,
    val name: String,
    val position: Int,
)

@Entity(tableName = "bookshelves", indices = [Index(value = ["name"], unique = true)])
data class BookshelfEntity(
    @PrimaryKey val id: String,
    val name: String,
    val coverAlbumId: String?,
    val createdAt: Long,
)

@Entity(
    tableName = "album_bookshelves",
    primaryKeys = ["albumId", "bookshelfId"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BookshelfEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookshelfId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("albumId"), Index("bookshelfId")],
)
data class AlbumBookshelfEntity(val albumId: String, val bookshelfId: String)
