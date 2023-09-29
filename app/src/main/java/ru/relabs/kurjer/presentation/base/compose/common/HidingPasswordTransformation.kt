package ru.relabs.kurjer.presentation.base.compose.common

import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun PasswordTransformationProvider(
    content: @Composable (VisualTransformation) -> Unit
) {
    val transform: MutableState<VisualTransformation?> = remember {
        mutableStateOf(HidingPasswordTransformation(mutableStateOf(null), ""))
    }
    LaunchedEffect(Unit) {
        transform.value = HidingPasswordTransformationWithLastChar(transform, "")
    }
    content(transform.value ?: VisualTransformation.None)
}

@Preview
@Composable
fun PasswordTextFieldSample() {
    var password by remember { mutableStateOf("") }

    PasswordTransformationProvider {
        TextField(value = password, onValueChange = { password = it }, visualTransformation = it)
    }
}


private class HidingPasswordTransformation(
    private val transform: MutableState<VisualTransformation?>,
    currentText: String,
    private val mask: Char = '\u2022'
) :
    VisualTransformation {
    private var lastText = currentText

    override fun filter(text: AnnotatedString): TransformedText {
        val transformation = if (text.length > lastText.length) {
            transform.value = HidingPasswordTransformationWithLastChar(transform, text.text, mask)
            text.maskWithLastChar(mask)
        } else {
            TransformedText(
                AnnotatedString(mask.toString().repeat(text.text.length)),
                OffsetMapping.Identity
            )
        }
        lastText = text.text
        return transformation
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HidingPasswordTransformation) return false
        if (mask != other.mask) return false
        return true
    }

    override fun hashCode(): Int {
        return mask.hashCode()
    }
}

private class HidingPasswordTransformationWithLastChar(
    private val transform: MutableState<VisualTransformation?>,
    currentText: String,
    private val mask: Char = '\u2022'
) :
    VisualTransformation {
    val scope = CoroutineScope(Dispatchers.Default)
    var job: Job? = null
    private var lastText = currentText

    override fun filter(text: AnnotatedString): TransformedText {
        job?.cancel()
        job = scope.launch {
            delay(1000)
            transform.value = HidingPasswordTransformation(transform, lastText, mask)
        }
        val transformation = if (text.text.length >= lastText.length) {
            text.maskWithLastChar(mask)
        } else {
            TransformedText(
                AnnotatedString(mask.toString().repeat(text.text.length)),
                OffsetMapping.Identity
            )
        }
        lastText = text.text
        return transformation
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HidingPasswordTransformationWithLastChar) return false
        if (mask != other.mask) return false
        return true
    }

    override fun hashCode(): Int {
        return mask.hashCode()
    }
}

private fun AnnotatedString.maskWithLastChar(mask: Char = '\u2022') = if (text.isEmpty())
    TransformedText(AnnotatedString(""), OffsetMapping.Identity)
else
    TransformedText(
        AnnotatedString(mask.toString().repeat(this.text.length - 1) + (this.text.last())),
        OffsetMapping.Identity
    )