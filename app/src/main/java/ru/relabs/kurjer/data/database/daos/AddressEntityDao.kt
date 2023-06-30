package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.relabs.kurjer.data.database.entities.AddressEntity

/**
 * Created by ProOrange on 30.08.2018.
 */
@Dao
interface AddressEntityDao {

    @get:Query("SELECT * FROM addresses")
    val all: List<AddressEntity>

    @Query("SELECT * FROM addresses WHERE id = :id")
    suspend fun getById(id: Int): AddressEntity?

    @Update
    suspend fun update(address: AddressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(address: AddressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(address: List<AddressEntity>)

    @Delete
    suspend fun delete(address: AddressEntity)
}