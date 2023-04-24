package ru.relabs.kurjer.presentation.base.compose.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeliveryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = ColorFuchsia),
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }

}