package ru.relabs.kurjer.ui.fragments


import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_login.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.ui.presenters.LoginPresenter

class LoginFragment : Fragment() {

    val presenter = LoginPresenter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    fun setRememberPasswordEnabled(enabled: Boolean){
        val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics).toInt()
        remember_password_text.setCompoundDrawables(
                context?.getDrawable(
                        if (enabled)
                            R.drawable.ic_check_circle_selected
                        else
                            R.drawable.ic_check_circle_unselected
                )?.apply {
                    setBounds(0, 0, size, size)
                }, null, null, null
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRememberPasswordEnabled(false)
        remember_password_text.setOnClickListener {
            presenter.onRememberPasswordClick()
        }
        login_button.setOnClickListener {
            presenter.onLoginClick(login_input.text.toString(), password_input.text.toString())
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = LoginFragment()
    }
}
