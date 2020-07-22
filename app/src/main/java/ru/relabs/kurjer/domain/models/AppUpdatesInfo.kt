package ru.relabs.kurjer.domain.models

data class AppUpdatesInfo(
    val required: AppUpdate?,
    val optional: AppUpdate?
)

data class AppUpdate(
    val version: Int,
    val url: String,
    val isRequired: Boolean
)
