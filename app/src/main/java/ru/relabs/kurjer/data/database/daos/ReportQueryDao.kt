package ru.relabs.kurjer.data.database.daos

import androidx.room.*
import ru.relabs.kurjer.data.database.entities.ReportQueryItemEntity

/**
 * Created by ProOrange on 06.09.2018.
 */
@Dao
interface ReportQueryDao {

    @get:Query("SELECT * FROM report_query")
    val all: List<ReportQueryItemEntity>

    @Query("SELECT * FROM report_query WHERE id = :id")
    fun getById(id: Int): ReportQueryItemEntity

    @Query("SELECT * FROM report_query WHERE task_id = :id")
    fun getByTaskId(id: Int): List<ReportQueryItemEntity>

    @Query("SELECT * FROM report_query WHERE task_item_id = :id")
    fun getByTaskItemId(id: Int): ReportQueryItemEntity?

    @Update
    fun update(address: ReportQueryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(address: ReportQueryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(address: List<ReportQueryItemEntity>)

    @Delete
    fun delete(address: ReportQueryItemEntity)
}