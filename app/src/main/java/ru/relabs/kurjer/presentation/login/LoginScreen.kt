package ru.relabs.kurjer.presentation.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.R
import ru.relabs.kurjer.data.models.auth.UserLogin
import ru.relabs.kurjer.presentation.base.compose.ElmScaffold
import ru.relabs.kurjer.presentation.base.compose.ElmScaffoldContext
import ru.relabs.kurjer.presentation.base.compose.common.CustomTextField
import ru.relabs.kurjer.presentation.base.compose.common.CustomTextKeyboardAction
import ru.relabs.kurjer.presentation.base.compose.common.DeliveryButton
import ru.relabs.kurjer.presentation.base.compose.common.LoadableContainer
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorGrayBase
import ru.relabs.kurjer.presentation.base.tea.ElmController
import ru.relabs.kurjer.utils.NetworkHelper

@Composable
fun LoginScreen(
    controller: ElmController<LoginContext, LoginState>
) = ElmScaffold(controller) {
    val isLoading by watchAsState { it.loaders > 0 }

    LoadableContainer(isLoading = isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            Version(
                modifier = Modifier
                    .align(Alignment.TopEnd)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.height(8.dp))
                Logo()
                Spacer(Modifier.height(16.dp))
                CredentialsInput()
            }
            LoginButton(
                modifier = Modifier
                    .align(alignment = Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun Version(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.app_version_label, BuildConfig.VERSION_CODE),
        color = ColorGrayBase,
        modifier = modifier
    )
}

@Composable
private fun Logo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(108.dp)
    )
}

@Composable
private fun ElmScaffoldContext<LoginContext, LoginState>.CredentialsInput(modifier: Modifier = Modifier) {
    val login by watchAsState { it.login.login }
    var loginInput by remember { mutableStateOf(login) }
    val password by watchAsState { it.password }
    var passwordInput by remember { mutableStateOf(password) }
    val isChecked by watchAsState { it.isPasswordRemembered }
    val interactionSource = remember { MutableInteractionSource() }

    val passwordFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    LaunchedEffect(loginInput) {
        sendMessage(LoginMessages.msgLoginChanged(UserLogin(loginInput)))
    }
    LaunchedEffect(passwordInput) {
        sendMessage(LoginMessages.msgPasswordChanged(passwordInput))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        CustomTextField(
            value = loginInput,
            onValueChange = { loginInput = it },
            placeholder = stringResource(R.string.login_placeholder),
            maxLines = 1,
            keyboardAction = CustomTextKeyboardAction.Next(passwordFocusRequester),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
        Spacer(Modifier.height(8.dp))
        CustomTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            placeholder = stringResource(R.string.password_placeholder),
            maxLines = 1,
            visualTransformation = PasswordVisualTransformation(),
            keyboardAction = CustomTextKeyboardAction.Done(
                { sendMessage(LoginMessages.msgLoginClicked(NetworkHelper.isNetworkEnabled(context))) },
                keyboardType = KeyboardType.Password
            ),
            focusRequester = passwordFocusRequester,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clickable(interactionSource = interactionSource, indication = null) { sendMessage(LoginMessages.msgRememberChanged()) }
                .padding(vertical = 4.dp)
        )
        {
            Image(
                painter = painterResource(if (!isChecked) R.drawable.ic_check_circle_unselected else R.drawable.ic_check_circle_selected),
                contentDescription = null,
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = false)
                ) { sendMessage(LoginMessages.msgRememberChanged()) }
            )
            Text(
                text = stringResource(R.string.remember_password),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                fontSize = 24.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun ElmScaffoldContext<LoginContext, LoginState>.LoginButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    DeliveryButton(
        text = stringResource(R.string.login_button_text).uppercase(),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        sendMessage(LoginMessages.msgLoginClicked(NetworkHelper.isNetworkEnabled(context)))
    }
}

@Composable
private fun ElmScaffoldContext<LoginContext, LoginState>.BackupDialogController(){

}