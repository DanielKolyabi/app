package ru.relabs.kurjer.utils.extensions

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import ru.relabs.kurjer.R

/**
 * Created by Daniil Kurchanov on 05.11.2019.
 */

fun Fragment.showToast(text: String) {
    Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
}

fun Fragment.showSnackbar(
    message: String,
    action: SnackbarAction? = null,
    onDismiss: (() -> Unit)? = null,
    onDisappear: (() -> Unit)? = null,
    length: Int? = null
) {
    this.view?.let {
        showSnackbar(it, message, action, onDismiss, onDisappear, length)
    }
}

fun showSnackbar(
    view: View,
    message: String,
    action: SnackbarAction? = null,
    onDismiss: (() -> Unit)? = null,
    onDisappear: (() -> Unit)? = null,
    length: Int? = null
) {
    view.let {
        val builder =
            Snackbar.make(
                it,
                message,
                length ?: Snackbar.LENGTH_SHORT.takeIf { action == null } ?: Snackbar.LENGTH_LONG)

        action?.let {
            builder.setAction(action.first) { action.second() }
        }

        builder.setActionTextColor(view.context.resources.getColorCompat(R.color.textGreen))
        if (onDismiss != null || onDisappear != null) {
            builder.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    if (event != DISMISS_EVENT_ACTION) {
                        onDismiss?.invoke()
                    } else {
                        onDisappear?.invoke()
                    }
                }
            })
        }

        builder.show()
    }
}

typealias SnackbarAction = Pair<String, () -> Unit>
