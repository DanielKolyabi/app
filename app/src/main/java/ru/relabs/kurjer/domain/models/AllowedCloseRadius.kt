package ru.relabs.kurjer.domain.models

sealed class AllowedCloseRadius {
    object NotRequired : AllowedCloseRadius()
    data class Required(val distance: Int) : AllowedCloseRadius()
}