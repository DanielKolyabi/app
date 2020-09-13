package ru.relabs.kurjer.presentation.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_login.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.relabs.kurjer.R
import ru.relabs.kurjer.data.models.auth.UserLogin
import ru.relabs.kurjer.utils.NetworkHelper
import ru.relabs.kurjer.presentation.base.TextChangeListener
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.tea.debugCollector
import ru.relabs.kurjer.presentation.base.tea.defaultController
import ru.relabs.kurjer.presentation.base.tea.rendersCollector
import ru.relabs.kurjer.presentation.base.tea.sendMessage
import ru.relabs.kurjer.utils.debug
import ru.relabs.kurjer.utils.extensions.showDialog


/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

class LoginFragment : BaseFragment() {

    private val controller = defaultController(LoginState(), LoginContext())
    private var renderJob: Job? = null

    private val loginTextWatcher = TextChangeListener {
        uiScope.sendMessage(controller, LoginMessages.msgLoginChanged(UserLogin(it)))
    }
    private val passwordTextWatcher = TextChangeListener {
        uiScope.sendMessage(controller, LoginMessages.msgPasswordChanged(it))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller.start(LoginMessages.msgInit())
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindControls(view)

        renderJob = uiScope.launch {
            val renders = listOf(
                LoginRenders.renderLogin(view.et_login, loginTextWatcher),
                LoginRenders.renderPassword(view.et_password, passwordTextWatcher),
                LoginRenders.renderCheckbox(view.cb_remember),
                LoginRenders.renderVersion(view.tv_version),
                LoginRenders.renderLoading(view.loading)
            )
            launch { controller.stateFlow().collect(rendersCollector(renders)) }
            launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
        }
        controller.context.errorContext.attach(view)
        controller.context.showOfflineLoginOffer = ::showLoginOfflineOffer
        controller.context.showError = ::showError
    }

    private fun showError(id: Int) {
        showDialog(
            id,
            R.string.ok to {}
        )
    }

    fun showLoginOfflineOffer() {
        showDialog(
            R.string.login_no_network,
            R.string.ok to {},
            R.string.login_offline to { uiScope.sendMessage(controller, LoginMessages.msgLoginOffline()) }
        )
    }

    private fun bindControls(view: View) {
        view.et_login.addTextChangedListener(loginTextWatcher)
        view.et_password.addTextChangedListener(passwordTextWatcher)
        view.cb_remember.setOnCheckedChangeListener { _, isChecked ->
            uiScope.sendMessage(controller, LoginMessages.msgRememberChanged(isChecked))
        }
        view.btn_login.setOnClickListener {
            uiScope.sendMessage(
                controller,
                LoginMessages.msgLoginClicked(
                    NetworkHelper.isNetworkEnabled(requireContext())
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderJob?.cancel()
        controller.context.showOfflineLoginOffer = {}
        controller.context.showError = {}
        controller.context.errorContext.detach()
    }

    override fun interceptBackPressed(): Boolean {
        return false
    }

    companion object {
        fun newInstance() = LoginFragment()
    }
}