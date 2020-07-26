package ru.relabs.kurjer.presentation.host.featureCheckers

import android.app.Activity
import android.content.Intent

abstract class FeatureChecker(internal var activity: Activity?) {
    abstract fun isFeatureEnabled(): Boolean
    abstract fun requestFeature()
    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}
    open fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {}

    fun onDestroy() {
        activity = null
    }
}