package ru.relabs.kurjer.network.models

/**
 * Created by Daniil Kurchanov on 06.01.2020.
 */
data class RadiusResponse(
        val locked: Boolean,
        val radius: Int
)