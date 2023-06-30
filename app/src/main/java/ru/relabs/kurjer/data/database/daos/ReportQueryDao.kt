package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.relabs.kurjer.data.database.entities.ReportQueryItemEntity

/**
 * Created by ProOrange on 06.09.2018.
 */
@Dao
interface ReportQueryDao {

    @get:Query("SELECT * FROM report_query")
    val all: List<ReportQueryItemEntity>

    @Query("SELECT * FROM report_query WHERE id = :id")
    suspend fun getById(id: Int): ReportQueryItemEntity

    @Query("SELECT * FROM report_query WHERE task_id = :id")
    suspend fun getByTaskId(id: Int): List<ReportQueryItemEntity>

    @Query("SELECT * FROM report_query WHERE task_item_id = :id")
    suspend fun getByTaskItemId(id: Int): ReportQueryItemEntity?

    @Update
    suspend fun update(address: ReportQueryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(address: ReportQueryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(address: List<ReportQueryItemEntity>)

    @Delete
    suspend fun delete(address: ReportQueryItemEntity)
}