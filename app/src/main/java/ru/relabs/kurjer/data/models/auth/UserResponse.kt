package ru.relabs.kurjer.data.models.auth

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserLogin(val login: String): Parcelable

data class UserResponse(
    @SerializedName("login") val login: String
)
