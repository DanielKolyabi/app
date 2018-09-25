package ru.relabs.kurjer.persistence.daos

import android.arch.persistence.room.*
import ru.relabs.kurjer.persistence.entities.SendQueryItemEntity

/**
 * Created by ProOrange on 06.09.2018.
 */
@Dao
interface SendQueryDao {

    @get:Query("SELECT * FROM send_query")
    val all: List<SendQueryItemEntity>

    @Query("SELECT * FROM send_query WHERE id = :id")
    fun getById(id: Int): SendQueryItemEntity

    @Update
    fun update(address: SendQueryItemEntity)

    @Insert
    fun insert(address: SendQueryItemEntity): Long

    @Insert
    fun insertAll(address: List<SendQueryItemEntity>)

    @Delete
    fun delete(address: SendQueryItemEntity)
}