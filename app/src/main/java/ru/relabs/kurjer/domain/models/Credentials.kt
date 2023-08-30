package ru.relabs.kurjer.domain.models

import ru.relabs.kurjer.data.models.auth.UserLogin
import java.io.Serializable


data class Credentials(
    val login: UserLogin,
    val password: String
) : Serializable