package ru.relabs.kurjer.presentation.base.compose.common.themes

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun DeliveryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(typography = Typography) {

        content()
    }
}

private val Typography = Typography(
    h1 = TextStyle(color = ColorGrayBase, letterSpacing = 0.sp),
    h2 = TextStyle(color = ColorGrayBase, letterSpacing = 0.sp),
    h3 = TextStyle(color = ColorGrayBase, letterSpacing = 0.sp),
    h4 = TextStyle(color = ColorGrayBase, letterSpacing = 0.sp),
    h5 = TextStyle(color = ColorGrayBase, letterSpacing = 0.sp),
    h6 = TextStyle(color = ColorGrayBase, letterSpacing = 0.sp),
    subtitle1 = TextStyle(color = ColorGrayBase, letterSpacing = 0.sp),
    subtitle2 = TextStyle(color = ColorGrayBase, letterSpacing = 0.sp),
    body1 = TextStyle(color = ColorGrayBase, letterSpacing = 0.sp, fontSize = 14.sp),
    body2 = TextStyle(color = ColorGrayBase, letterSpacing = 0.sp),
    button = TextStyle(color = Color.White, letterSpacing = 0.sp, fontWeight = FontWeight.Medium),
    caption = TextStyle(color = ColorGrayBase, letterSpacing = 0.sp),
    overline = TextStyle(color = ColorGrayBase, letterSpacing = 0.sp),
)
