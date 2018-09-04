package ru.relabs.kurjer.persistence.entities

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import ru.relabs.kurjer.models.AddressModel

/**
 * Created by ProOrange on 31.08.2018.
 */
@Entity(tableName = "addresses")
data class AddressEntity(
        @PrimaryKey
        var id: Int,
        @ColumnInfo(name = "street")
        var street: String,
        @ColumnInfo(name = "house")
        var house: Int
) {
    fun toAddressModel(): AddressModel {
        return AddressModel(id, street, house)
    }
}