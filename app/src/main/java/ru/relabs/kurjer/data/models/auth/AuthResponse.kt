package ru.relabs.kurjer.data.models.auth

import com.google.gson.annotations.SerializedName

/**
 * Created by ProOrange on 05.09.2018.
 */

data class AuthResponse(
    @SerializedName("user") val user: UserResponse,
    @SerializedName("token") val token: String
)