package ru.relabs.kurjer.presentation.base.compose.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppBar(
    painterId: Int,
    title: String,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(ColorPrimary)
    ) {
        Icon(
            painter = painterResource(painterId),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(48.dp)
                .padding(8.dp)
                .clickable {
                    onBackClicked()
                }
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            maxLines = 2,
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp
        )
    }
}

@Composable
fun AppBarLoadableContainer(
    isLoading: Boolean,
    painterId: Int,
    title: String,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier)
    {
        AppBar(painterId = painterId, title = title, onBackClicked = onBackClicked)
        LoadableContainer(isLoading = isLoading, content = content)
    }

}