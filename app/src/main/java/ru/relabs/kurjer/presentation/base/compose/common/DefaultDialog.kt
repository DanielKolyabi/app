package ru.relabs.kurjer.presentation.base.compose.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorFuchsia

@Composable
fun DefaultDialog(
    text: @Composable () -> Unit,
    acceptButton: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    declineButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = Color.White,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Column {
                    title?.let { it() }
                    text()
                    Spacer(Modifier.height(30.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        declineButton?.let {
                            it()
                            Spacer(Modifier.width(40.dp))
                        }
                        acceptButton()
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultDialog(
    text: String,
    title: String? = null,
    acceptButton: Pair<String, () -> Unit>,
    declineButton: Pair<String, () -> Unit>? = null,
    onDismiss: () -> Unit,
    dismissible: Boolean = false,
    textColor: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    DefaultDialog(
        onDismissRequest = {
            if (dismissible)
                onDismiss()
        },
        acceptButton = {
            Text(
                text = acceptButton.first.uppercase(),
                color = ColorFuchsia,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(interactionSource = interactionSource, indication = null) {
                    acceptButton.second()
                    onDismiss()
                }
            )
        },
        declineButton = f@{
            if (declineButton == null) return@f
            Text(
                text = declineButton.first.uppercase(),
                color = ColorFuchsia,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(interactionSource = interactionSource, indication = null) {
                    declineButton.second()
                    onDismiss()
                }
            )
        },
        text = {
            Text(
                text = text,
                color = textColor ?: Color.Black,
                fontSize = 16.sp
            )
        },
        title = title?.let {
            {
                Text(
                    text = title,
                    color = Color.Black,
                    fontSize = 18.sp
                )
            }
        }
    )
}

@Preview
@Composable
fun DefaultDialogPreview() {
    DefaultDialog(text = "asdlifhasdiolfuhasodfiu", acceptButton = "Да" to {}, onDismiss = { /*TODO*/ })
}
