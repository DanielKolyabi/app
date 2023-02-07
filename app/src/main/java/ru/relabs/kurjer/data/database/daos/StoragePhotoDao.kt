package ru.relabs.kurjer.data.database.daos

import androidx.room.*
import ru.relabs.kurjer.data.database.entities.storage.StorageReportPhotoEntity

@Dao
interface StoragePhotoDao {

    @get:Query("SELECT * FROM storage_report_photos")
    val all: List<StorageReportPhotoEntity>

    @Query("SELECT * FROM storage_report_photos WHERE id = :id")
    fun getById(id: Int): StorageReportPhotoEntity

    @Query("SELECT * FROM storage_report_photos WHERE report_id = :id")
    fun getByStorageReportId(id: Int): List<StorageReportPhotoEntity>

    @Update
    fun update(photo: StorageReportPhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(photo: StorageReportPhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(photo: List<StorageReportPhotoEntity>)

    @Delete
    fun delete(photos: StorageReportPhotoEntity)

    @Query("DELETE FROM storage_report_photos WHERE report_id = :id")
    fun deleteByStorageReportId(id: Int)

    @Query("DELETE FROM storage_report_photos WHERE id = :photoId")
    fun deleteById(photoId: Int)
}