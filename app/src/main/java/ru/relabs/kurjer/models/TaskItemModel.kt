package ru.relabs.kurjer.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.relabs.kurjer.data.database.entities.TaskItemEntity

@Parcelize
data class EntranceModel(
    val num: Int,
    var coupleEnabled: Boolean
): Parcelable

@Parcelize
data class TaskItemModel(
    var address: AddressModel,
    var state: Int,
    var id: Int,
    var notes: List<String>,
    var entrances: List<EntranceModel>,
    var subarea: Int,
    var bypass: Int,
    var copies: Int,
    var needPhoto: Boolean,
    var entrancesData: List<EntranceDataModel>
) : Parcelable