package ru.relabs.kurjer.domain.storage

/**
 * Created by Daniil Kurchanov on 20.11.2019.
 */
class AuthTokenStorage(
    private val appPreferences: AppPreferences
) {
    private var token: String? = null

    fun saveToken(token: String) =
        appPreferences.saveAuthToken(token).also {
            this.token = token
        }

    fun getToken(): String? =
        token ?: appPreferences.getAuthToken()?.also { saveToken(it) }

    fun resetToken() =
        appPreferences.resetAuthToken().also {
            this.token = null
        }
}