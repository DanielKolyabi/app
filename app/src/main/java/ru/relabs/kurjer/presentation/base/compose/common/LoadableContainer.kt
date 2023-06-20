package ru.relabs.kurjer.presentation.base.compose.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import ru.relabs.kurjer.R
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorFuchsia

@Composable
fun LoadableContainer(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    gpsLoading: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray.copy(0.5f)).gesturesDisabled(true)) {
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .zIndex(999f)
                ) {
                    CircularProgressIndicator(
                        color = ColorFuchsia,
                    )
                    if (gpsLoading) {
                        Text(text = stringResource(R.string.report_gps_loading))
                    }
                }
            }
        }
    }
}