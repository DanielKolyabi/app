package ru.relabs.kurjer.domain.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppUpdatesInfo(
    val required: AppUpdate?,
    val optional: AppUpdate?
): Parcelable

@Parcelize
data class AppUpdate(
    val version: Int,
    val url: Uri,
    val isRequired: Boolean
): Parcelable
