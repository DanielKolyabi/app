package ru.relabs.kurjer.utils.extensions

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import java.math.BigInteger
import java.security.MessageDigest

/**
 * Created by Daniil Kurchanov on 06.11.2019.
 */

fun Context.spToPx(sp: Number): Float{
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), this.resources.displayMetrics)
}

fun Context.dpToPx(dp: Number): Float{
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), this.resources.displayMetrics)
}

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}
fun Context.dip(dp: Number): Float {
    return resources.dip(dp)
}

fun Resources.dip(dp: Number): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), displayMetrics)
}

fun View.dip(dp: Number): Float {
    return resources.dip(dp)
}