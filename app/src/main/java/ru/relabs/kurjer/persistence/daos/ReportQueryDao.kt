package ru.relabs.kurjer.persistence.daos

import android.arch.persistence.room.*
import ru.relabs.kurjer.persistence.entities.ReportQueryItemEntity

/**
 * Created by ProOrange on 06.09.2018.
 */
@Dao
interface ReportQueryDao {

    @get:Query("SELECT * FROM report_query")
    val all: List<ReportQueryItemEntity>

    @Query("SELECT * FROM report_query WHERE id = :id")
    fun getById(id: Int): ReportQueryItemEntity

    @Update
    fun update(address: ReportQueryItemEntity);

    @Insert
    fun insert(address: ReportQueryItemEntity);

    @Insert
    fun insertAll(address: List<ReportQueryItemEntity>);

    @Delete
    fun delete(address: ReportQueryItemEntity);
}