package ru.relabs.kurjer.ui.presenters

import android.content.Context
import android.util.Log
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.joda.time.DateTime
import retrofit2.HttpException
import ru.relabs.kurjer.*
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI.api
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.network.models.ErrorUtils
import ru.relabs.kurjer.persistence.PersistenceHelper
import ru.relabs.kurjer.ui.fragments.LoginFragment
import ru.relabs.kurjer.utils.activity
import ru.relabs.kurjer.utils.application
import java.util.*

/**
 * Created by ProOrange on 24.08.2018.
 */
const val INVALID_TOKEN_ERROR_CODE = 4

class LoginPresenter(val fragment: LoginFragment) {

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

        launch(UI) {
            val db = application().database
            val sharedPref = application().getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
            fragment.setLoginButtonLoading(true)
            try {
                val time = DateTime().toString("yyyy-MM-dd'T'HH:mm:ss")

                val response = if (!authByToken)
                    api.login(login, pwd, application().deviceUUID, time).await()
                else
                    api.loginByToken(pwd, application().deviceUUID, time).await()

                if (response.error != null) {
                    if(response.error.code == INVALID_TOKEN_ERROR_CODE){
                        response.error.message = "Введите пароль"
                    }
                    fragment.activity()?.showError("Ошибка №${response.error.code}\n${response.error.message}")
                    return@launch
                }
                application().currentLocation = GPSCoordinatesModel(0.0, 0.0, Date(0))
                application().user = UserModel.Authorized(response.user!!.login, response.token!!)
                if (isPasswordRemembered) {
                    application().storeUserCredentials()
                } else {
                    application().restoreUserCredentials()
                }
                application().sendDeviceInfo(null)
                if (sharedPref.getString("last_login", "") != response.user.login) {
                    Log.d("login", "Clear local database. User changed. Last login ${sharedPref.getString("last_login", "")}. New login ${response.user.login}")
                    withContext(CommonPool) {
                        db.taskDao().all.forEach {
                            PersistenceHelper.closeTaskById(db, it.id)
                        }
                    }
                }
                sharedPref.edit().putString("last_login", response.user.login).apply()
                (fragment.activity as? MainActivity)?.showTaskListScreen(!authByToken)

            } catch (e: HttpException) {
                e.printStackTrace()

                if (e.code() == 502) {
                    showOfflineLoginOffer()
                    return@launch
                }

                val err = ErrorUtils.getError(e)
                fragment.activity()?.showError("Ошибка №${err.code}.\n${err.message}")
            } catch (e: Exception) {
                e.printStackTrace()
                showOfflineLoginOffer()
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
        application().user = user
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