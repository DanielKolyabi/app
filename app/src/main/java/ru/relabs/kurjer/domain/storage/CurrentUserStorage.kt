package ru.relabs.kurjer.domain.storage

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import ru.relabs.kurjer.data.models.auth.UserLogin

/**
 * Created by Daniil Kurchanov on 21.01.2020.
 */
class CurrentUserStorage(private val appPreferences: AppPreferences) {
    val currentUser: BroadcastChannel<UserLogin?> = BroadcastChannel(Channel.CONFLATED)

    fun saveCurrentUserLogin(login: UserLogin) {
        appPreferences.saveCurrentUserLogin(login)
        currentUser.trySend(login)
    }

    fun getCurrentUserLogin(): UserLogin? {
        val user = appPreferences.getCurrentUserLogin()
        currentUser.trySend(user)
        return user
    }

    fun resetCurrentUserLogin() {
        currentUser.trySend(null)
        appPreferences.resetCurrentUserLogin()
    }

}