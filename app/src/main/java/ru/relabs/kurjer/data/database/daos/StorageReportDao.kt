package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.relabs.kurjer.data.database.entities.storage.StorageReportEntity

@Dao
interface StorageReportDao {

    @get:Query("SELECT * FROM storage_reports")
    val all: List<StorageReportEntity>

    @Query("SELECT * FROM storage_reports WHERE id = :id")
    suspend fun getById(id: Int): StorageReportEntity

    @Query("SELECT * FROM storage_reports WHERE storage_id = :id AND is_closed = :isClosed ")
    suspend fun getOpenedByStorageId(id: Int, isClosed: Boolean): List<StorageReportEntity>?

    @Query("SELECT * FROM storage_reports WHERE storage_id = :id AND is_closed = :isClosed AND task_ids = :taskIds")
    suspend fun getOpenedByStorageIdWithTaskIds(id: Int, taskIds: List<Int>, isClosed: Boolean): List<StorageReportEntity>?

    @Query("DELETE FROM storage_reports WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Update
    suspend fun update(report: StorageReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: StorageReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<StorageReportEntity>)

    @Delete
    suspend fun delete(report: StorageReportEntity)

    @Delete
    suspend fun deleteList(reports: List<StorageReportEntity>)
}