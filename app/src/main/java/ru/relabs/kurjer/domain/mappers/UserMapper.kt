package ru.relabs.kurjer.domain.mappers

import ru.relabs.kurjer.data.models.auth.UserLogin
import ru.relabs.kurjer.data.models.auth.UserResponse
import ru.relabs.kurjer.domain.models.User

object UserMapper {
    fun fromRaw(raw: UserResponse): User = User(
        login = UserLogin(raw.login)
    )
}
