package ru.relabs.kurjer.presentation.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.R
import ru.relabs.kurjer.data.models.auth.UserLogin
import ru.relabs.kurjer.presentation.base.compose.ElmScaffoldContext


@Composable
fun ElmScaffoldContext<TasksContext, TasksState>.TasksDrawer(userLogin: UserLogin?, onCrashLogClicked: () -> Unit ,  modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item { DrawerHeader() }
        item { DrawerItem(text = stringResource(R.string.pause), onCLick = { sendMessage(TasksMessages.msgPauseClicked()) }) }
        item { DrawerItem(text = stringResource(R.string.menu_info), onCLick = { onCrashLogClicked() }) }
        item { DrawerItem(text = stringResource(R.string.menu_uuid), onCLick = { sendMessage(TasksMessages.msgCopyDeviceUUID()) }) }
        item { DrawerItem(text = stringResource(R.string.menu_logout), onCLick = { sendMessage(TasksMessages.msgLogout()) }) }
        item {
            DrawerItem(
                text = stringResource(R.string.menu_bottom_info, BuildConfig.VERSION_CODE, userLogin?.login ?: "-"),
                onCLick = {})
        }
    }
}

@Composable
private fun DrawerHeader(modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(54.dp)) {
        Text(
            text = stringResource(R.string.menu_title),
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .padding(start = 8.dp)
        )
    }
}

@Composable
private fun DrawerItem(text: String, modifier: Modifier = Modifier, onCLick: () -> Unit) {
    Box(modifier = modifier
        .fillMaxWidth()
        .clickable { onCLick() }) {
        Text(text = text, color = Color.Black, fontSize = 16.sp, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp))
    }
}