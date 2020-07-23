package ru.relabs.kurjer.domain.models

import ru.relabs.kurjer.data.models.auth.UserLogin

data class User(
    val login: UserLogin
)