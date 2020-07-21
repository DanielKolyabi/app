package ru.relabs.kurjer.domain.storage

import ru.relabs.kurjer.data.models.auth.UserLogin

/**
 * Created by Daniil Kurchanov on 21.01.2020.
 */
class CurrentUserStorage(private val appPreferences: AppPreferences) {
    fun saveCurrentUserLogin(login: UserLogin) =
        appPreferences.saveCurrentUserLogin(login)

    fun getCurrentUserLogin(): UserLogin? =
        appPreferences.getCurrentUserLogin()

    fun resetCurrentUserLogin() =
        appPreferences.resetCurrentUserLogin()

}