package ru.relabs.kurjer.domain.useCases

import ru.relabs.kurjer.data.models.auth.UserLogin
import ru.relabs.kurjer.data.models.common.EitherE
import ru.relabs.kurjer.domain.models.User
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.RadiusRepository
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.domain.storage.CurrentUserStorage
import ru.relabs.kurjer.utils.fmap

class LoginUseCase(
    private val deliveryRepository: DeliveryRepository,
    private val currentUserStorage: CurrentUserStorage,
    private val databaseRepository: DatabaseRepository,
    private val radiusRepository: RadiusRepository,
    private val authTokenStorage: AuthTokenStorage,
    private val pauseRepository: PauseRepository
){

    suspend fun loginOffline(login: UserLogin, token: String): User {
        loginInternal(login, token)
        return User(login)
    }

    suspend fun login(login: UserLogin, password: String): EitherE<User> {
        return deliveryRepository.login(login, password).fmap { (user, token) ->
            loginInternal(user.login, token)
            user
        }
    }

    suspend fun login(token: String): EitherE<User> {
        return deliveryRepository.login(token).fmap { (user, token) ->
            loginInternal(user.login, token)
            user
        }
    }

    private suspend fun loginInternal(login: UserLogin, token: String){
        val lastUserLogin = currentUserStorage.getCurrentUserLogin()
        if (lastUserLogin != login) {
            databaseRepository.clearTasks()
            radiusRepository.resetData()
            pauseRepository.resetData()
        }
        authTokenStorage.saveToken(token)
        currentUserStorage.saveCurrentUserLogin(login)
    }
}