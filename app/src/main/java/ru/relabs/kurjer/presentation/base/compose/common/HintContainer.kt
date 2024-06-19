package ru.relabs.kurjer.presentation.base.compose.common

import android.graphics.Color
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.repositories.TextSizeStorage
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorGradientEnd
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorGradientStart

@Composable
fun HintContainer(hintText: String, textSizeStorage: TextSizeStorage, modifier: Modifier = Modifier, minHeight: Dp = 100.dp,maxHeight: Dp = 510.dp) {
    val textSize by remember { textSizeStorage.textSize }.collectAsState()
    var expanded by remember { mutableStateOf(true) }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier
            .fillMaxWidth()
            .animateContentSize()
            .then(if(expanded) Modifier.heightIn(30.dp, maxHeight).wrapContentHeight() else Modifier.height(minHeight))

            .clickable(interactionSource = interactionSource, indication = null) {
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
                HtmlText(html = hintText, textSize = textSize.toFloat(), textColor = Color.BLACK)
            }
        }
    }
}