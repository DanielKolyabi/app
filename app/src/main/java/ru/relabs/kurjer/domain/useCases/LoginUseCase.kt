package ru.relabs.kurjer.domain.useCases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.data.models.auth.UserLogin
import ru.relabs.kurjer.data.models.common.EitherE
import ru.relabs.kurjer.domain.models.User
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.RadiusRepository
import ru.relabs.kurjer.domain.storage.AppPreferences
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.domain.storage.CurrentUserStorage
import ru.relabs.kurjer.utils.fmap

class LoginUseCase(
    private val deliveryRepository: DeliveryRepository,
    private val currentUserStorage: CurrentUserStorage,
    private val databaseRepository: DatabaseRepository,
    private val radiusRepository: RadiusRepository,
    private val authTokenStorage: AuthTokenStorage,
    private val pauseRepository: PauseRepository,
    private val appPreferences: AppPreferences
){

    fun isAutologinEnabled() = appPreferences.getUserAutologinEnabled()

    suspend fun loginOffline(): User? {
        val savedLogin = currentUserStorage.getCurrentUserLogin() ?: return null
        val savedToken = authTokenStorage.getToken() ?: return null

        loginInternal(savedLogin, savedToken, offline = true)
        return User(savedLogin)
    }

    suspend fun login(login: UserLogin, password: String, remember: Boolean): EitherE<User> {
        appPreferences.setUserAutologinEnabled(remember)
        return deliveryRepository.login(login, password).fmap { (user, token) ->
            loginInternal(user.login, token, offline = false)
            user
        }
    }

    suspend fun login(token: String): EitherE<User> {
        return deliveryRepository.login(token).fmap { (user, token) ->
            loginInternal(user.login, token, offline = false)
            user
        }
    }

    private suspend fun loginInternal(login: UserLogin, token: String, offline: Boolean) = withContext(Dispatchers.IO){
        val lastUserLogin = currentUserStorage.getCurrentUserLogin()
        if (lastUserLogin != login) {
            databaseRepository.clearTasks()
            radiusRepository.resetData()
            pauseRepository.resetData()
        }
        authTokenStorage.saveToken(token)
        currentUserStorage.saveCurrentUserLogin(login)

        radiusRepository.startRemoteUpdating()
        pauseRepository.loadLastPausesRemote()
        deliveryRepository.updateDeviceIMEI()
        deliveryRepository.updatePushToken()
    }

    fun logout() {
        appPreferences.setUserAutologinEnabled(false)
        authTokenStorage.resetToken()
        currentUserStorage.resetCurrentUserLogin()
    }
}