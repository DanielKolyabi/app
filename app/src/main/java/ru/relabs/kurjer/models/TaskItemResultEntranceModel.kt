package ru.relabs.kurjer.models

data class TaskItemResultEntranceModel(
        val id: Int,
        val taskItemResult: TaskItemResultModel,
        val entrance: Int,
        val state: Int
)
