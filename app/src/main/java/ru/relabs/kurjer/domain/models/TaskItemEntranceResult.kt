package ru.relabs.kurjer.domain.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class ReportEntranceSelection(
    val isEuro: Boolean,
    val isWatch: Boolean,
    val isStacked: Boolean,
    val isRejected: Boolean
): Parcelable

@Parcelize
data class TaskItemEntranceId(val id: Int): Parcelable

@Parcelize
data class TaskItemEntranceResult(
    val id: TaskItemEntranceId,
    val taskItemResultId: TaskItemResultId,
    val entranceNumber: EntranceNumber,
    val selection: ReportEntranceSelection
): Parcelable