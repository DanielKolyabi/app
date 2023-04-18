package ru.relabs.kurjer.presentation.base.compose.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun LoadableContainer(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.then(if (isLoading) Modifier.background(Color.DarkGray.copy(0.5f)) else Modifier)) {
        content()
        if (isLoading) {
            CircularProgressIndicator(
                color = ColorFuchsia,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}