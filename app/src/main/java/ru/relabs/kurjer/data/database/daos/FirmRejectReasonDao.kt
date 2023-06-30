package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.relabs.kurjer.data.database.entities.FirmRejectReason

/**
 * Created by ProOrange on 30.08.2018.
 */
@Dao
interface FirmRejectReasonDao {

    @get:Query("SELECT * FROM firm_reject_reason")
    val all: List<FirmRejectReason>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(address: List<FirmRejectReason>)

    @Query("DELETE FROM firm_reject_reason")
    suspend fun clear()
}