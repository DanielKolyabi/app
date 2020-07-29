package ru.relabs.kurjer.presentation.taskDetails

import ru.relabs.kurjer.domain.models.Task

interface IExaminedConsumer {
    fun onExamined(task: Task)
}