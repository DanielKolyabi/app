package ru.relabs.kurjer.network.models

/**
 * Created by ProOrange on 05.09.2018.
 */

data class AuthResponseModel(
        val user: UserResponseModel?,
        val token: String?,
        override val error: ResponseErrorModel?
) : ResponseWithErrorModel()