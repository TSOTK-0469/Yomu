package cn.yomu.reader.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YomuDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        YomuDatabase::class.java,
    )

    @Test
    fun migration2To3PreservesLibraryAndAddsLocalMetadata() {
        helper.createDatabase(2).apply {
            execSQL(
                "INSERT INTO mount_sources " +
                    "(id, treeUri, directoryUri, name, mode, available, createdAt) " +
                    "VALUES ('mount', 'tree', 'directory', 'Pictures', 'RECURSIVE', 1, 10)",
            )
            execSQL(
                "INSERT INTO albums " +
                    "(id, mountId, directoryUri, name, path, defaultCoverUri, customCoverUri, " +
                    "pageCount, lastReadUri, lastReadIndex, hidden, updatedAt) " +
                    "VALUES ('album', 'mount', 'album-uri', 'Source name', 'Pictures/Album', " +
                    "'cover', NULL, 12, NULL, -1, 0, 20)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(3, listOf(YomuDatabase.MIGRATION_2_3))

        migrated.prepare("SELECT name, path, lastSuccessfulRefreshAt, lastRefreshFailed FROM mount_sources").use { statement ->
            statement.step()
            assertEquals("Pictures", statement.getText(0))
            assertEquals("", statement.getText(1))
            assertNull(if (statement.isNull(2)) null else statement.getText(2))
            assertEquals(0L, statement.getLong(3))
        }
        migrated.prepare("SELECT name, customName, pageCount FROM albums").use { statement ->
            statement.step()
            assertEquals("Source name", statement.getText(0))
            assertNull(if (statement.isNull(1)) null else statement.getText(1))
            assertEquals(12L, statement.getLong(2))
        }
        migrated.close()
    }
}
