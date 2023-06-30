package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ru.relabs.kurjer.data.database.entities.SendQueryItemEntity

/**
 * Created by ProOrange on 06.09.2018.
 */
@Dao
interface SendQueryDao {

    @get:Query("SELECT * FROM send_query")
    val all: List<SendQueryItemEntity>

    @Query("SELECT * FROM send_query WHERE id = :id")
    suspend fun getById(id: Int): SendQueryItemEntity

    @Update
    suspend fun update(address: SendQueryItemEntity)

    @Insert
    suspend fun insert(address: SendQueryItemEntity): Long

    @Insert
    suspend fun insertAll(address: List<SendQueryItemEntity>)

    @Delete
    suspend fun delete(address: SendQueryItemEntity)
}