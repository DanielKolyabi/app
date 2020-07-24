package ru.relabs.kurjer.presentation.base.fragment

/**
 * Created by Daniil Kurchanov on 20.11.2019.
 */

interface IFragmentStyleable {
    val navDrawerLocked: Boolean
    val isFullScreen: Boolean
}

data class AppBarSettings(
    val navDrawerLocked: Boolean = true,
    val isFullScreen: Boolean = false
)