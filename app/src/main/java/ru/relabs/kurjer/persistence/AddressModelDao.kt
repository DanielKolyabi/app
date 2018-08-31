package ru.relabs.kurjer.persistence

import android.arch.persistence.room.*
import ru.relabs.kurjer.models.AddressModel
import ru.relabs.kurjer.models.TaskModel

/**
 * Created by ProOrange on 30.08.2018.
 */
@Dao
interface AddressModelDao {

    @get:Query("SELECT * FROM addresses")
    val all: List<AddressModel>

    @Query("SELECT * FROM addresses WHERE id = :id")
    fun getById(id: Int): AddressModel
    @Update
    fun update(address: AddressModel);
    @Insert
    fun insert(address: AddressModel);
    @Insert
    fun insertAll(address: List<AddressModel>);
    @Delete
    fun delete(address: AddressModel);
}