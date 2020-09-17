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
        currentUser.offer(login)
    }

    fun getCurrentUserLogin(): UserLogin? {
        val user = appPreferences.getCurrentUserLogin()
        currentUser.offer(user)
        return user
    }

    fun resetCurrentUserLogin() {
        currentUser.offer(null)
        appPreferences.resetCurrentUserLogin()
    }

}