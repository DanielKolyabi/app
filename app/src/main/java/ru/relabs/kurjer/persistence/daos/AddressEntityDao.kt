package ru.relabs.kurjer.persistence.daos

import android.arch.persistence.room.*
import ru.relabs.kurjer.persistence.entities.AddressEntity

/**
 * Created by ProOrange on 30.08.2018.
 */
@Dao
interface AddressEntityDao {

    @get:Query("SELECT * FROM addresses")
    val all: List<AddressEntity>

    @Query("SELECT * FROM addresses WHERE id = :id")
    fun getById(id: Int): AddressEntity?

    @Update
    fun update(address: AddressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(address: AddressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(address: List<AddressEntity>)

    @Delete
    fun delete(address: AddressEntity)
}