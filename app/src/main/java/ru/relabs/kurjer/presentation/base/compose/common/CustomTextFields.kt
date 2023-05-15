package ru.relabs.kurjer.presentation.base.compose.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.relabs.kurjer.R
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorFuchsia
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorGrayBase

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
    val lineHeight by animateDpAsState(if (isFocused) 2.dp else 1.dp)
    val (options, actions) = keyboardAction.toKeyboardProperties()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        maxLines = maxLines,
        textStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box {
                Box(modifier = Modifier.padding(vertical = 10.dp)) {
                    if (value.isEmpty() && placeholder != null) {
                        Box(contentAlignment = Alignment.CenterStart) {
                            Text(
                                text = placeholder,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                color = ColorGrayBase.copy(alpha = 0.8f)
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
                        .height(lineHeight)
                        .align(Alignment.BottomCenter)
                        .background(lineColor)
                )
            }
        },
        visualTransformation = visualTransformation,
        keyboardActions = actions,
        keyboardOptions = options,
        cursorBrush = SolidColor(ColorFuchsia.copy(alpha = 0.8F)),
        modifier = modifier
            .focusRequester(focusRequester)
    )
}

@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onClearClicked: () -> Unit,
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
    val lineHeight by animateDpAsState(if (isFocused) 2.dp else 1.dp)
    val (options, actions) = keyboardAction.toKeyboardProperties()


    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        maxLines = maxLines,
        textStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null)
                        Box(modifier = Modifier.padding(vertical = 10.dp)) {
                            if (value.isEmpty() && placeholder != null) {
                                Box(contentAlignment = Alignment.CenterStart) {
                                    Text(
                                        text = placeholder,
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium,
                                        color = ColorGrayBase.copy(alpha = 0.8f)
                                    )
                                    innerTextField()
                                }
                            } else {
                                innerTextField()
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(lineHeight)
                            .align(Alignment.BottomCenter)
                            .background(lineColor)
                    )
                }
                if (value.isNotEmpty())
                    Icon(painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                        modifier = Modifier.clickable { onClearClicked() })
            }
        },
        visualTransformation = visualTransformation,
        keyboardActions = actions,
        keyboardOptions = options,
        cursorBrush = SolidColor(ColorFuchsia.copy(alpha = 0.8F)),
        modifier = modifier
            .focusRequester(focusRequester)
    )
}

@Composable
fun DescriptionTextField(
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
    val (options, actions) = keyboardAction.toKeyboardProperties()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        maxLines = maxLines,
        textStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box {
                Box(modifier = Modifier.padding(vertical = 10.dp)) {
                    if (value.isEmpty() && placeholder != null) {
                        Box(contentAlignment = Alignment.CenterStart) {
                            Text(
                                text = placeholder,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                color = ColorGrayBase.copy(alpha = 0.8f)
                            )
                            innerTextField()
                        }
                    } else {
                        innerTextField()
                    }
                }
            }
        },
        visualTransformation = visualTransformation,
        keyboardActions = actions,
        keyboardOptions = options,
        cursorBrush = SolidColor(ColorFuchsia.copy(alpha = 0.8F)),
        modifier = modifier
            .focusRequester(focusRequester)
    )

}