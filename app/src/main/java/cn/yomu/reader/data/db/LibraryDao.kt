package cn.yomu.reader.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface LibraryDao {
    @Query("SELECT * FROM mount_sources ORDER BY createdAt")
    suspend fun mounts(): List<MountEntity>

    @Query("SELECT * FROM mount_sources WHERE id = :id")
    suspend fun mount(id: String): MountEntity?

    @Query("SELECT * FROM albums")
    suspend fun albums(): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun album(id: String): AlbumEntity?

    @Query("SELECT * FROM albums WHERE mountId = :mountId")
    suspend fun albumsForMount(mountId: String): List<AlbumEntity>

    @Query("SELECT COUNT(*) FROM albums WHERE mountId = :mountId AND hidden = 0")
    suspend fun visibleAlbumCount(mountId: String): Int

    @Query("SELECT * FROM bookshelves ORDER BY createdAt")
    suspend fun bookshelves(): List<BookshelfEntity>

    @Query("SELECT * FROM bookshelves WHERE id = :id")
    suspend fun bookshelf(id: String): BookshelfEntity?

    @Query("SELECT * FROM album_bookshelves")
    suspend fun memberships(): List<AlbumBookshelfEntity>

    @Query("SELECT bookshelfId FROM album_bookshelves WHERE albumId = :albumId")
    suspend fun bookshelfIdsForAlbum(albumId: String): List<String>

    @Upsert suspend fun upsertMount(value: MountEntity)
    @Upsert suspend fun upsertAlbums(values: List<AlbumEntity>)
    @Upsert suspend fun upsertBookshelf(value: BookshelfEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMemberships(values: List<AlbumBookshelfEntity>)

    @Delete suspend fun deleteAlbums(values: List<AlbumEntity>)

    @Query("DELETE FROM mount_sources WHERE id = :id")
    suspend fun deleteMount(id: String)

    @Query("DELETE FROM bookshelves WHERE id = :id")
    suspend fun deleteBookshelf(id: String)

    @Query("DELETE FROM album_bookshelves WHERE albumId = :albumId")
    suspend fun deleteMembershipsForAlbum(albumId: String)

    @Query("DELETE FROM album_bookshelves WHERE albumId = :albumId AND bookshelfId = :bookshelfId")
    suspend fun deleteMembership(albumId: String, bookshelfId: String)

    @Query("UPDATE albums SET hidden = :hidden WHERE id = :albumId")
    suspend fun setAlbumHidden(albumId: String, hidden: Boolean)

    @Query("UPDATE albums SET customCoverUri = :uri, updatedAt = :updatedAt WHERE id = :albumId")
    suspend fun setAlbumCover(albumId: String, uri: String?, updatedAt: Long)

    @Query("UPDATE albums SET defaultCoverUri = :uri, customCoverUri = NULL, updatedAt = :updatedAt WHERE id = :albumId")
    suspend fun repairAlbumCover(albumId: String, uri: String, updatedAt: Long)

    @Query("UPDATE albums SET lastReadUri = :uri, lastReadIndex = :index WHERE id = :albumId")
    suspend fun saveReadingPosition(albumId: String, uri: String, index: Int)

    @Query("UPDATE mount_sources SET available = :available WHERE id = :mountId")
    suspend fun setMountAvailable(mountId: String, available: Boolean)
}
