package ru.relabs.kurjer.presentation.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import ru.relabs.kurjer.R
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.tea.defaultController
import ru.relabs.kurjer.presentation.base.tea.sendMessage
import ru.relabs.kurjer.utils.extensions.showDialog


/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

class LoginFragment : BaseFragment() {

    private val controller = defaultController(LoginState(), LoginContext())

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
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                LoginScreen(controller)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

    override fun onDestroyView() {
        super.onDestroyView()
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