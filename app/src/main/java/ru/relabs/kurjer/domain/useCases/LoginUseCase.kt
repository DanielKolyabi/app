package ru.relabs.kurjer.domain.useCases

import ru.relabs.kurjer.domain.models.User
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.RadiusRepository
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.domain.storage.CurrentUserStorage

class LoginUseCase(
    private val authTokenStorage: AuthTokenStorage,
    private val currentUserStorage: CurrentUserStorage,
    private val databaseRepository: DatabaseRepository,
    private val radiusRepository: RadiusRepository,
    private val pauseRepository: PauseRepository
){

    suspend fun logIn(user: User, token: String){
        val lastUserLogin = currentUserStorage.getCurrentUserLogin()
        if(lastUserLogin != user.login){
            databaseRepository.clearTasks()
            radiusRepository.resetData()
            pauseRepository.resetData()
        }
        authTokenStorage.saveToken(token)
        currentUserStorage.saveCurrentUserLogin(user.login)
    }

    fun logOut(){
        authTokenStorage.resetToken()
        currentUserStorage.resetCurrentUserLogin()
    }
}