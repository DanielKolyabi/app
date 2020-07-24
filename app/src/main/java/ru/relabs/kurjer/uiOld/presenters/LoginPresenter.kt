package ru.relabs.kurjer.uiOld.presenters

import android.content.Context
import kotlinx.android.synthetic.main.fragment_login_old.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.ErrorButtonsListener
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.data.models.auth.UserLogin
import ru.relabs.kurjer.data.models.common.DomainException
import ru.relabs.kurjer.domain.models.User
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.RadiusRepository
import ru.relabs.kurjer.domain.useCases.LoginUseCase
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.uiOld.fragments.LoginFragment
import ru.relabs.kurjer.utils.*
import java.util.*

/**
 * Created by ProOrange on 24.08.2018.
 */
const val INVALID_TOKEN_ERROR_CODE = 4

class LoginPresenter(
    val fragment: LoginFragment,
    private val radiusRepository: RadiusRepository,
    private val pauseRepository: PauseRepository,
    private val database: AppDatabase,
    private val deliveryRepository: DeliveryRepository,
    private val loginUseCase: LoginUseCase,
    private val dbRep: DatabaseRepository
) {

    private var isPasswordRemembered = false
    private var authByToken = false

    fun onRememberPasswordClick() {
        setRememberPassword(!isPasswordRemembered)
        fragment.setRememberPasswordEnabled(isPasswordRemembered)
    }

    fun setRememberPassword(enabled: Boolean) {
        isPasswordRemembered = enabled
    }

    fun onLoginClick(login: String, pwd: String) {
        if (!NetworkHelper.isNetworkAvailable(fragment.context)) {
            showOfflineLoginOffer()
            return
        }

        GlobalScope.launch(Dispatchers.Main) {
            val sharedPref = application().getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
            fragment.setLoginButtonLoading(true)

            val response = if (!authByToken)
                deliveryRepository.login(UserLogin(login), pwd)
            else
                deliveryRepository.login(pwd)

            when (response) {
                is Right -> {
                    application().currentLocation = GPSCoordinatesModel(0.0, 0.0, Date(0))
                    application().user = response.value

                    application().sendDeviceInfo(null)
                    radiusRepository.startRemoteUpdating()
                    pauseRepository.loadLastPausesRemote()
                    (fragment.activity as? MainActivity)?.showTaskListScreen(!authByToken, 0, true)
                }
                is Left -> when (val r = response.value) {
                    is DomainException.ApiException ->
                        fragment.activity()?.showError("Ошибка №${r.error.code}\n${r.error.message}")
                    else -> {
                        showOfflineLoginOffer()
                    }
                }
            }
            fragment.setLoginButtonLoading(false)
        }
    }

    fun showOfflineLoginOffer() {
        fragment.activity()?.showError("Нет ответа от сервера.",
            object : ErrorButtonsListener {
                override fun negativeListener() {
                    val status = loginOffline()
                    if (!status) {
                        fragment.activity()?.showError("Невозможно войти оффлайн. Необходима авторизация через сервер.")
                        return
                    }
                    (fragment.activity as? MainActivity)?.showTaskListScreen(false)
                }

                override fun positiveListener() {}
            }
            , "Ок", "Войти Оффлайн")
    }

    fun loginOffline(): Boolean {
        val user = application().getUserCredentials()
        user ?: return false

        application().user = User(UserLogin(user.login))
        GlobalScope.launch {
            loginUseCase.logIn(User(UserLogin(user.login)), user.token)
        }
        return true
    }

    fun resetAuthByToken() {
        authByToken = false
    }

    fun loadUserCredentials() {
        val credentials = application().getUserCredentials()
        credentials ?: return
        fragment.login_input.setText(credentials.login)
        fragment.password_input.setText(credentials.token)
        authByToken = true
        setRememberPassword(true)
        fragment.shouldResetRememberOnInput = true
        fragment.setRememberPasswordEnabled(true)
    }
}