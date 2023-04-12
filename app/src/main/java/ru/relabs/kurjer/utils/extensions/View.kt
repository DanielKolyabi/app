package ru.relabs.kurjer.utils.extensions

import android.app.Activity
import android.content.Context
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Created by Daniil Kurchanov on 21.11.2019.
 */

fun View.showKeyboard(ctx: Context?) {
    (ctx?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
        ?.showSoftInput(this, 0)
}

fun View.hideKeyboard(ctx: Context?) {
    (ctx?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
        ?.hideSoftInputFromWindow(this.windowToken, 0)
}

fun Activity.hideKeyboard() {
    currentFocus?.hideKeyboard(this)
}

var View.visible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

fun EditText.renderText(text: String, listener: TextWatcher? = null) {
    if (this.text.toString() != text) {
        listener?.let { removeTextChangedListener(it) }
        setText(text)
        setSelection(text.length)
        listener?.let { addTextChangedListener(it) }
    }
}

fun RecyclerView.onFragmentDestroyView() {
    this.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            this@onFragmentDestroyView.adapter = null
        }

        override fun onViewDetachedFromWindow(v: View) {}
    })
}