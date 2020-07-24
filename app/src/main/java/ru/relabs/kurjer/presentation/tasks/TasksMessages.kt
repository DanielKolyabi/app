package ru.relabs.kurjer.presentation.tasks

import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgState

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

object TasksMessages {
    fun msgInit(): TasksMessage = msgEffects(
        { it },
        { listOf(TasksEffects.effectInit()) }
    )
}