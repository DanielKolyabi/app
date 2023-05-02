package ru.relabs.kurjer.presentation.base.compose.common

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.repositories.TextSizeRepository

@Composable
fun HintContainer(hintText: String, textSizeRepository: TextSizeRepository, modifier: Modifier = Modifier) {
    val textSize by textSizeRepository.textSize.collectAsState()

    Box(
        modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(8.dp)
                .background(brush = Brush.verticalGradient(listOf(ColorGradientEnd, ColorGradientStart)))
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(brush = Brush.verticalGradient(listOf(ColorGradientStart, ColorGradientEnd)))
                .align(Alignment.BottomCenter)
        )
        Row {
            Column {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(start = 8.dp, top = 8.dp)
                )
                Spacer(Modifier.height(12.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 24.dp, height = 48.dp)
                        .padding(start = 8.dp, top = 12.dp, bottom = 12.dp)
                )
                Spacer(Modifier.height(48.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_minus),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 24.dp, height = 48.dp)
                        .padding(start = 8.dp, top = 12.dp, bottom = 12.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = hintText,
                    modifier = Modifier
                )
            }
        }

    }
}