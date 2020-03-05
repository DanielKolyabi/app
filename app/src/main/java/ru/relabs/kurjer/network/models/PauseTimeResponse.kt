package ru.relabs.kurjer.network.models

/**
 * Created by Daniil Kurchanov on 06.01.2020.
 */
data class PauseDurationsResponse(
        val loading: Long,
        val lunch: Long
)

data class PauseTimeResponse(
        val start: PauseTimes,
        val end: PauseTimes
)

data class PauseTimes(
        val loading: Long,
        val lunch: Long
)

