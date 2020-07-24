package ru.relabs.kurjer.presentation.tasks

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */
object TasksEffects {

    fun effectInit(): TasksEffect = { c, s ->
        //TODO: Load tasks (try to load from web)
    }
}