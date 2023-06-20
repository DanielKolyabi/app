package ru.relabs.kurjer.presentation.base.compose.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorPrimary

@Composable
fun BasicAppBar(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = Color.White,
    startIcon: Painter? = null,
    startIconClicked: () -> Unit = {},
    endIcon: Painter? = null,
    endIconClicked: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(ColorPrimary)
    ) {
        startIcon?.let {
            Icon(
                painter = it,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        startIconClicked()
                    }
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            maxLines = 2,
            color = titleColor,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        endIcon?.let {
            Icon(
                painter = it,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        endIconClicked()
                    }
            )
            Spacer(Modifier.width(8.dp))
        }
    }
}


@Composable
fun DefaultAppBar(
    painterId: Int,
    title: String,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = Color.White
) {
    BasicAppBar(
        title = title,
        titleColor = titleColor,
        startIcon = painterResource(painterId),
        startIconClicked = onBackClicked,
        modifier = modifier
    )
}

@Composable
fun TasksAppBar(
    title: String,
    menuIcon: Painter,
    menuIconClicked: () -> Unit,
    refreshIcon: Painter,
    refreshIconClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    BasicAppBar(
        title = title,
        startIcon = menuIcon,
        startIconClicked = menuIconClicked,
        endIcon = refreshIcon,
        endIconClicked = refreshIconClicked,
        modifier = modifier
    )
}

@Composable
fun AppBarLoadableContainer(
    isLoading: Boolean,
    painterId: Int,
    title: String,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    gpsLoading: Boolean = false,
    titleColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        DefaultAppBar(painterId = painterId, title = title, titleColor = titleColor, onBackClicked = onBackClicked)
        LoadableContainer(isLoading = isLoading, gpsLoading = gpsLoading, content = content)
    }

}