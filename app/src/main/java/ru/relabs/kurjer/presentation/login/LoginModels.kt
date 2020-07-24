package ru.relabs.kurjer.presentation.login

import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.presentation.base.tea.*

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

data class LoginState(
    val data: Any? = null
)

class LoginContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

}

typealias LoginMessage = ElmMessage<LoginContext, LoginState>
typealias LoginEffect = ElmEffect<LoginContext, LoginState>
typealias LoginRender = ElmRender<LoginState>