package ru.relabs.kurjer.domain.models

sealed class AllowedCloseRadius {
    data class NotRequired(val distance: Int) : AllowedCloseRadius()
    data class Required(val distance: Int) : AllowedCloseRadius()
}