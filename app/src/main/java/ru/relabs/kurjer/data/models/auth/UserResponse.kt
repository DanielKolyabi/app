package ru.relabs.kurjer.data.models.auth

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class UserLogin(
    @SerializedName("login")
    val login: String
): Parcelable, Serializable

data class UserResponse(
    @SerializedName("login") val login: String
)
