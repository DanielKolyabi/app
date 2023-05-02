package ru.relabs.kurjer.domain.repositories

import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TextSizeRepository(val sharedPreferences: SharedPreferences) {
    val textSize: StateFlow<Int> get() = _textSize
    private val _textSize: MutableStateFlow<Int> = MutableStateFlow(16)

    fun increase() {

    }

    fun decrease() {

    }
}