package ru.relabs.kurjer.network.models

/**
 * Created by Daniil Kurchanov on 06.01.2020.
 */
data class PauseCheckResponse(
        val loading: Int,
        val lunch: Int,
        val control: Int
)