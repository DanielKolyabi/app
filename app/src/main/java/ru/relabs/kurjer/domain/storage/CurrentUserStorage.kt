package ru.relabs.kurjer.domain.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.relabs.kurjer.data.models.auth.UserLogin

/**
 * Created by Daniil Kurchanov on 21.01.2020.
 */
class CurrentUserStorage(private val appPreferences: AppPreferences) {
    private val _currentUser: MutableStateFlow<UserLogin?> = MutableStateFlow(null)
    val currentUser: StateFlow<UserLogin?> = _currentUser

    fun saveCurrentUserLogin(login: UserLogin) {
        appPreferences.saveCurrentUserLogin(login)
        _currentUser.tryEmit(login)
    }

    fun getCurrentUserLogin(): UserLogin? {
        val user = appPreferences.getCurrentUserLogin()
        _currentUser.tryEmit(user)
        return user
    }

    fun resetCurrentUserLogin() {
        _currentUser.tryEmit(null)
        appPreferences.resetCurrentUserLogin()
    }

}