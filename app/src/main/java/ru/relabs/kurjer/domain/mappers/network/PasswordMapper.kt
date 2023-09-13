package ru.relabs.kurjer.domain.mappers.network

import ru.relabs.kurjer.data.models.auth.PasswordResponse

object PasswordMapper {
    fun fromRaw(response: PasswordResponse): String = response.password
}
