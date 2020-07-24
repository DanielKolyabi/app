package ru.relabs.kurjer.presentation.login

import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.data.models.auth.UserLogin
import ru.relabs.kurjer.domain.useCases.LoginUseCase
import ru.relabs.kurjer.presentation.base.tea.*

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

data class LoginState(
    val login: UserLogin = UserLogin(""),
    val password: String = "",
    val isPasswordRemembered: Boolean = false,
    val loaders: Int = 0
)

class LoginContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

    var showOfflineLoginOffer: () -> Unit = {}
    var showOfflineLoginError: () -> Unit = {}
}

typealias LoginMessage = ElmMessage<LoginContext, LoginState>
typealias LoginEffect = ElmEffect<LoginContext, LoginState>
typealias LoginRender = ElmRender<LoginState>