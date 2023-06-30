package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.relabs.kurjer.data.database.entities.storage.StorageReportRequestEntity

@Dao
interface StorageReportRequestDao {
    @get:Query("SELECT * FROM storage_report_query")
    val all: List<StorageReportRequestEntity>

    @Query("SELECT * FROM storage_report_query WHERE id = :id")
    suspend fun getById(id: Int): StorageReportRequestEntity

    @Query("SELECT * FROM storage_report_query WHERE task_id = :id")
    suspend fun getByTaskId(id: Int): List<StorageReportRequestEntity>

    @Query("SELECT * FROM storage_report_query WHERE storage_report_id = :storageReportId")
    suspend fun getByReportId(storageReportId: Int): List<StorageReportRequestEntity>

    @Update
    suspend fun update(address: StorageReportRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: StorageReportRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<StorageReportRequestEntity>)

    @Delete
    suspend fun delete(request: StorageReportRequestEntity)

}