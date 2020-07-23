package ru.relabs.kurjer.domain.useCases

import ru.relabs.kurjer.domain.models.User
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.domain.storage.CurrentUserStorage

class LoginUseCase(
    private val authTokenStorage: AuthTokenStorage,
    private val currentUserStorage: CurrentUserStorage
){

    fun logIn(user: User, token: String){
        authTokenStorage.saveToken(token)
        currentUserStorage.saveCurrentUserLogin(user.login)
    }

    fun logOut(){
        authTokenStorage.resetToken()
        currentUserStorage.resetCurrentUserLogin()
    }
}