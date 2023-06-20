package ru.relabs.kurjer.domain.repositories

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TextSizeStorage(private val preferences: SharedPreferences) {
    private val _textSize: MutableStateFlow<Int> = MutableStateFlow(preferences.getInt(FONT_SIZE_KEY, 12))
    val textSize: StateFlow<Int> = _textSize

    init {
        preferences.getInt(FONT_SIZE_KEY, -1).takeIf { it == -1 }?.let {
            val oldValue = preferences.getFloat("hint_font_size", 12f).toInt()
            updateValue(oldValue)
        }
    }

    fun increase() {
        if (_textSize.value >= 26) return
        val targetValue = _textSize.value + 2
        updateValue(targetValue)
    }

    fun decrease() {
        if (_textSize.value <= 12) return
        val targetValue = _textSize.value - 2
        updateValue(targetValue)
    }

    private fun updateValue(newValue: Int) {
        _textSize.tryEmit(newValue)
        preferences.edit { putInt(FONT_SIZE_KEY, newValue) }
    }


    companion object {
        private const val FONT_SIZE_KEY = "hint_font_size_key"
    }
}