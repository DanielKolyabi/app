package ru.relabs.kurjer.data.database.daos

import androidx.room.*
import ru.relabs.kurjer.data.database.entities.storage.StorageReportRequestEntity

@Dao
interface StorageReportRequestDao {
    @get:Query("SELECT * FROM storage_report_query")
    val all: List<StorageReportRequestEntity>

    @Query("SELECT * FROM storage_report_query WHERE id = :id")
    fun getById(id: Int): StorageReportRequestEntity

    @Query("SELECT * FROM storage_report_query WHERE task_id = :id")
    fun getByTaskId(id: Int): List<StorageReportRequestEntity>

    @Query("SELECT * FROM storage_report_query WHERE storage_report_id = :storageReportId")
    fun getByReportId(storageReportId: Int): List<StorageReportRequestEntity>

    @Update
    fun update(address: StorageReportRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(request: StorageReportRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(requests: List<StorageReportRequestEntity>)

    @Delete
    fun delete(request: StorageReportRequestEntity)

}