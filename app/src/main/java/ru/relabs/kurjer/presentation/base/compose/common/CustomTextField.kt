package ru.relabs.kurjer.presentation.base.compose.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

sealed class CustomTextKeyboardAction {
    protected abstract val keyboardType: KeyboardType

    data class Next(
        val focusRequester: FocusRequester,
        override val keyboardType: KeyboardType = KeyboardType.Text
    ) : CustomTextKeyboardAction()

    data class Done(
        val onDone: () -> Unit,
        override val keyboardType: KeyboardType = KeyboardType.Text
    ) : CustomTextKeyboardAction()

    object Default : CustomTextKeyboardAction() {
        override val keyboardType: KeyboardType
            get() = KeyboardType.Text
    }

    @Composable
    fun toKeyboardProperties(): Pair<KeyboardOptions, KeyboardActions> {
        val kbOptions = remember {
            KeyboardOptions(keyboardType = this.keyboardType).let {
                when (this) {
                    Default -> it
                    is Done -> it.copy(imeAction = ImeAction.Done)
                    is Next -> it.copy(imeAction = ImeAction.Next)
                }
            }
        }

        val kbAction = remember {
            when (this) {
                Default -> KeyboardActions.Default
                is Done -> KeyboardActions(onDone = { onDone() })
                is Next -> KeyboardActions(onNext = { focusRequester.requestFocus() })
            }
        }

        return kbOptions to kbAction
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    maxLines: Int = Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardAction: CustomTextKeyboardAction = CustomTextKeyboardAction.Default,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val lineColor by animateColorAsState(if (isFocused) ColorFuchsia else ColorGrayBase)
    val (options, actions) = keyboardAction.toKeyboardProperties()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        maxLines = maxLines,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box {
                Box(modifier = Modifier.padding(vertical = 12.dp)) {
                    if (value.isEmpty() && placeholder != null) {
                        Box(contentAlignment = Alignment.CenterStart) {
                            Text(
                                text = placeholder,
                                fontFamily = FontFamily.SansSerif,
                                color = ColorGrayBase
                            )
                            innerTextField()
                        }
                    } else {
                        innerTextField()
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.BottomCenter)
                        .background(lineColor)
                )
            }
        },
        visualTransformation = visualTransformation,
        keyboardActions = actions,
        keyboardOptions = options,
        cursorBrush = SolidColor(ColorFuchsia.copy(alpha = 0.8F)),
        modifier = modifier.focusRequester(focusRequester)
    )
}