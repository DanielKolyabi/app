package ru.relabs.kurjer.network

import android.content.Context
import android.net.ConnectivityManager
import android.support.v4.app.Fragment

/**
 * Created by ProOrange on 05.09.2018.
 */

object NetworkHelper{
    fun isNetworkAvailable(fragment: Fragment): Boolean{
        fragment.activity ?: false
        return (fragment.activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo.isConnectedOrConnecting
    }
}