package ru.relabs.kurjer.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class PhotoId(val id: Int): Parcelable

@Parcelize
data class TaskItemPhoto(
    val id: PhotoId,
    val UUID: String,
    val taskItemId: TaskItemId,
    val entranceNumber: EntranceNumber
) : Parcelable