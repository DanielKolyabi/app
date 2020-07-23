package ru.relabs.kurjer.ui.fragments


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_login.*
import org.koin.android.ext.android.inject
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.ErrorButtonsListener
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.RadiusRepository
import ru.relabs.kurjer.domain.useCases.LoginUseCase
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.ui.helpers.setVisible
import ru.relabs.kurjer.ui.presenters.LoginPresenter
import ru.relabs.kurjer.utils.activity
import ru.relabs.kurjer.utils.application

class LoginFragment : Fragment() {

    private val radiusRepository: RadiusRepository by inject()
    private val pauseRepository: PauseRepository by inject()
    private val database: AppDatabase by inject()
    private val deliveryRepository: DeliveryRepository by inject()
    private val loginUseCase: LoginUseCase by inject()

    val presenter = LoginPresenter(
        this,
        radiusRepository,
        pauseRepository,
        database,
        deliveryRepository,
        loginUseCase
    )

    var shouldResetRememberOnInput = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    fun setRememberPasswordEnabled(enabled: Boolean) {
        presenter.setRememberPassword(enabled)
        val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics).toInt()
        remember_password_text?.setCompoundDrawables(
            context?.getDrawable(
                if (enabled)
                    R.drawable.checked_checkbox
                else
                    R.drawable.unchecked_checkbox
            )?.apply {
                setBounds(0, 0, size, size)
            }, null, null, null
        )
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRememberPasswordEnabled(true)
        remember_password_text.setOnClickListener {
            presenter.onRememberPasswordClick()
        }
        login_button?.isEnabled = true
        login_button.setOnClickListener {
            if (application().lastRequiredAppVersion > BuildConfig.VERSION_CODE) {
                activity()?.showError("Необходимо обновить приложение.", object : ErrorButtonsListener {
                    override fun positiveListener() {
                        activity()?.checkUpdates()
                    }
                }, "Обновить")
                return@setOnClickListener
            }
            if (!NetworkHelper.isNetworkEnabled(context)) {
                activity()?.showNetworkDisabledError()
                return@setOnClickListener
            }
            presenter.onLoginClick(login_input.text.toString(), password_input.text.toString())
        }

        app_version.text = resources.getString(R.string.app_version_label, BuildConfig.VERSION_NAME)

        val myTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!shouldResetRememberOnInput) {
                    return
                }
                if (s?.length == 0) {
                    shouldResetRememberOnInput = false
                }
                setRememberPasswordEnabled(false)
                presenter.resetAuthByToken()
            }
        }

        login_input.addTextChangedListener(myTextWatcher)
        password_input.addTextChangedListener(myTextWatcher)

        presenter.loadUserCredentials()
    }

    fun setLoginButtonLoading(state: Boolean) {
        login_button?.isEnabled = !state
        loading?.setVisible(state)
    }

    companion object {
    }
}
