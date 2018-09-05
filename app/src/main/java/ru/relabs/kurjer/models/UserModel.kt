package ru.relabs.kurjer.models

/**
 * Created by ProOrange on 05.09.2018.
 */
sealed class UserModel {
    object Unauthorized : UserModel()
    data class Authorized(
            val login: String,
            val token: String
    ) : UserModel()
}