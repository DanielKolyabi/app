package ru.relabs.kurjer.data.database.daos

import androidx.room.*
import ru.relabs.kurjer.data.database.entities.storage.StorageReportEntity

@Dao
interface StorageReportDao {

    @get:Query("SELECT * FROM storage_reports")
    val all: List<StorageReportEntity>

    @Query("SELECT * FROM storage_reports WHERE id = :id")
    fun getById(id: Int): StorageReportEntity

    @Query("SELECT * FROM storage_reports WHERE storage_id = :id AND is_closed = :isClosed")
    fun getOpenedByStorageId(id: Int, isClosed: Boolean): List<StorageReportEntity>?

    @Query("DELETE FROM storage_reports WHERE id = :id")
    fun deleteById(id: Int)

    @Update
    fun update(report: StorageReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(report: StorageReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(reports: List<StorageReportEntity>)

    @Delete
    fun delete(report: StorageReportEntity)

    @Delete
    fun deleteList(reports: List<StorageReportEntity>)
}