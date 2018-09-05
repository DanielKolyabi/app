package ru.relabs.kurjer.ui.presenters

import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import retrofit2.HttpException
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.activity
import ru.relabs.kurjer.application
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI.api
import ru.relabs.kurjer.network.models.ErrorUtils
import ru.relabs.kurjer.ui.fragments.LoginFragment

/**
 * Created by ProOrange on 24.08.2018.
 */
class LoginPresenter(val fragment: LoginFragment) {

    private var isPasswordRemembered = false
    private var authByToken = false

    fun onRememberPasswordClick() {
        isPasswordRemembered = !isPasswordRemembered
        fragment.setRememberPasswordEnabled(isPasswordRemembered)
    }

    fun onLoginClick(login: String, pwd: String) {
        launch(UI) {
            fragment.setLoginButtonLoading(true)
            try {
                val response = if (!authByToken)
                    api.login(login, pwd, fragment.application()!!.deviceUUID).await()
                else
                    api.loginByToken(pwd, fragment.application()!!.deviceUUID).await()

                if (response.error != null) {
                    fragment.activity().showError("Ошибка №${response.error.code}\n${response.error.message}")
                    return@launch
                }
                fragment.application()!!.user = UserModel.Authorized(response.user!!.login, response.token!!)
                if (isPasswordRemembered) {
                    fragment.application()?.storeUserCredentials()
                } else {
                    fragment.application()?.restoreUserCredentials()
                }

                (fragment.activity as? MainActivity)?.showTaskListScreen()
            } catch (e: HttpException) {
                e.printStackTrace()
                val err = ErrorUtils.getError(e)
                fragment.activity().showError("Ошибка №${err.code}.\n${err.message}")
            } catch (e: Exception) {
                e.printStackTrace()
                fragment.activity().showError("Нет ответа от сервера.")
            }
            fragment.setLoginButtonLoading(false)
        }
    }

    fun resetAuthByToken() {
        authByToken = false
    }

    fun loadUserCredentials() {
        val credentials = fragment.application()?.getUserCredentials()
        credentials ?: return
        fragment.login_input.setText(credentials.login)
        fragment.password_input.setText(credentials.token)
        authByToken = true
        isPasswordRemembered = true
        fragment.setRememberPasswordEnabled(true)
    }
}