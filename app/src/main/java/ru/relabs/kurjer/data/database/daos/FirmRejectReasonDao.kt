package ru.relabs.kurjer.data.database.daos

import androidx.room.*
import ru.relabs.kurjer.data.database.entities.AddressEntity
import ru.relabs.kurjer.data.database.entities.FirmRejectReason

/**
 * Created by ProOrange on 30.08.2018.
 */
@Dao
interface FirmRejectReasonDao {

    @get:Query("SELECT * FROM firm_reject_reason")
    val all: List<FirmRejectReason>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(address: List<FirmRejectReason>)

    @Query("DELETE FROM firm_reject_reason")
    fun clear()
}