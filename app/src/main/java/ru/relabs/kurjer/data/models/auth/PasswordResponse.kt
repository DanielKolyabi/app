package ru.relabs.kurjer.data.models.auth

import com.google.gson.annotations.SerializedName

data class PasswordResponse(
   @SerializedName("password") val password: String
)
