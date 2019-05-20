package ru.relabs.kurjer.ui.fragments


import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_login.*
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.R
import ru.relabs.kurjer.activity
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.ui.helpers.setVisible
import ru.relabs.kurjer.ui.presenters.LoginPresenter

class LoginFragment : Fragment() {

    val presenter = LoginPresenter(this)

    var shouldResetRememberOnInput = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRememberPasswordEnabled(true)
        remember_password_text.setOnClickListener {
            presenter.onRememberPasswordClick()
        }
        login_button?.isEnabled = true
        login_button.setOnClickListener {
            if(!NetworkHelper.isNetworkEnabled(context)){
                activity()?.showNetworkDisabledError()
                return@setOnClickListener
            }
            presenter.onLoginClick(login_input.text.toString(), password_input.text.toString())
        }

        app_version.text = resources.getString(R.string.app_version_label, BuildConfig.VERSION_CODE)

        val myTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!shouldResetRememberOnInput) {
                    return
                }
                if(s?.length == 0){
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
