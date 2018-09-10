package ru.relabs.kurjer.network.models

data class StatusResponse(
        val status: Boolean,
        val error: ErrorModel?
)
