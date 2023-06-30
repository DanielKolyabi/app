package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.relabs.kurjer.data.database.entities.storage.StorageReportPhotoEntity

@Dao
interface StoragePhotoDao {

    @get:Query("SELECT * FROM storage_report_photos")
    val all: List<StorageReportPhotoEntity>

    @Query("SELECT * FROM storage_report_photos WHERE id = :id")
    suspend fun getById(id: Int): StorageReportPhotoEntity

    @Query("SELECT * FROM storage_report_photos WHERE report_id = :id")
    suspend fun getByStorageReportId(id: Int): List<StorageReportPhotoEntity>

    @Update
    suspend fun update(photo: StorageReportPhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: StorageReportPhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photo: List<StorageReportPhotoEntity>)

    @Delete
    suspend fun delete(photos: StorageReportPhotoEntity)

    @Query("DELETE FROM storage_report_photos WHERE report_id = :id")
    suspend fun deleteByStorageReportId(id: Int)

    @Query("DELETE FROM storage_report_photos WHERE id = :photoId")
    suspend fun deleteById(photoId: Int)
}