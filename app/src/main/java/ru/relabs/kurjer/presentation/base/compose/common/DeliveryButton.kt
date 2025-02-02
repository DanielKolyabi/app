package ru.relabs.kurjer.presentation.base.compose.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorFuchsia

@Composable
fun DeliveryButton(
    text: String,
    backgroundColor: Color = ColorFuchsia,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = backgroundColor),
        shape = RoundedCornerShape(2.dp),
        contentPadding = contentPadding,
        modifier = modifier.zIndex(1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 3.dp)
        )
    }
}
