package ru.relabs.kurjer.domain.storage

import ru.relabs.kurjer.data.models.auth.UserLogin
import ru.relabs.kurjer.domain.models.Credentials

class SavedUserStorage(private val appPreferences: AppPreferences) {

    fun saveCredentials(login: UserLogin, password: String) {
        appPreferences.backUpUserCredentials(login, password)
    }

    fun getCredentials(): Credentials? = appPreferences.getBackedUpCredentials()

    fun saveToken(token: String) {
        appPreferences.backUpToken(token)
    }

    fun getToken(): String? = appPreferences.getBackedUpToken()

}

