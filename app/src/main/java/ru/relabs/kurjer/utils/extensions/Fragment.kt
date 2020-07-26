package ru.relabs.kurjer.utils.extensions

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

/**
 * Created by Daniil Kurchanov on 10.12.2019.
 */

fun Fragment.hideKeyboard() {
    activity?.hideKeyboard()
}

fun Activity.showDialog(
    message: String,
    positiveButton: Pair<Int, () -> Unit>? = null,
    negativeButton: Pair<Int, () -> Unit>? = null,
    cancelable: Boolean = false
): AlertDialog = AlertDialog.Builder(this)
    .setMessage(message)
    .setCancelable(cancelable)
    .apply {
        if (positiveButton != null) {
            setPositiveButton(positiveButton.first) { _, _ -> positiveButton.second() }
        }
        if (negativeButton != null) {
            setNegativeButton(negativeButton.first) { _, _ -> negativeButton.second() }
        }
    }
    .show()

fun Activity.showDialog(
    messageId: Int,
    positiveButton: Pair<Int, () -> Unit>? = null,
    negativeButton: Pair<Int, () -> Unit>? = null,
    cancelable: Boolean = false
): AlertDialog = AlertDialog.Builder(this)
    .setMessage(resources.getString(messageId))
    .setCancelable(cancelable)
    .apply {
        if (positiveButton != null) {
            setPositiveButton(positiveButton.first) { _, _ -> positiveButton.second() }
        }
        if (negativeButton != null) {
            setNegativeButton(negativeButton.first) { _, _ -> negativeButton.second() }
        }
    }
    .show()

fun Fragment.showDialog(
    messageId: Int,
    positiveButton: Pair<Int, () -> Unit>? = null,
    negativeButton: Pair<Int, () -> Unit>? = null,
    cancelable: Boolean = false
): AlertDialog = showDialog(
    resources.getString(messageId),
    positiveButton,
    negativeButton,
    cancelable
)

fun Fragment.showDialog(
    message: String,
    positiveButton: Pair<Int, () -> Unit>? = null,
    negativeButton: Pair<Int, () -> Unit>? = null,
    cancelable: Boolean = false
): AlertDialog = AlertDialog.Builder(requireContext())
    .setMessage(message)
    .setCancelable(cancelable)
    .apply {
        if (positiveButton != null) {
            setPositiveButton(positiveButton.first) { _, _ -> positiveButton.second() }
        }
        if (negativeButton != null) {
            setNegativeButton(negativeButton.first) { _, _ -> negativeButton.second() }
        }
    }
    .show()