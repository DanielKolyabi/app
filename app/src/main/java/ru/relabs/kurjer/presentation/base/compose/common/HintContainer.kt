package ru.relabs.kurjer.presentation.base.compose.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.repositories.TextSizeStorage
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorGradientEnd
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorGradientStart

@Composable
fun HintContainer(hintText: String, textSizeStorage: TextSizeStorage, maxHeight: Dp = 250.dp, modifier: Modifier = Modifier) {
    val textSize by remember { textSizeStorage.textSize }.collectAsState()
    var expanded by remember { mutableStateOf(true) }
    val containerHeight by animateDpAsState(if (expanded) maxHeight else 30.dp)

    Box(
        modifier
            .fillMaxWidth()
            .height(containerHeight)
            .clickable {
                expanded = !expanded
            }
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
                        .clickable { textSizeStorage.increase() }
                )
                Spacer(Modifier.height(48.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_minus),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 24.dp, height = 48.dp)
                        .padding(start = 8.dp, top = 12.dp, bottom = 12.dp)
                        .clickable { textSizeStorage.decrease() }
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
                    color = Color.Black,
                    fontSize = textSize.sp,
                    modifier = Modifier
                )
            }
        }
    }
}