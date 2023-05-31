package ru.relabs.kurjer.presentation.tasks

import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskState
import ru.relabs.kurjer.presentation.base.compose.ElmScaffold
import ru.relabs.kurjer.presentation.base.compose.ElmScaffoldContext
import ru.relabs.kurjer.presentation.base.compose.common.DeliveryButton
import ru.relabs.kurjer.presentation.base.compose.common.LoaderItem
import ru.relabs.kurjer.presentation.base.compose.common.SearchTextField
import ru.relabs.kurjer.presentation.base.compose.common.TasksAppBar
import ru.relabs.kurjer.presentation.base.compose.common.gesturesDisabled
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorLoaderBackground
import ru.relabs.kurjer.presentation.base.compose.common.themes.HtmlText
import ru.relabs.kurjer.presentation.base.tea.ElmController

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun TasksScreen(controller: ElmController<TasksContext, TasksState>, onMenuClick: () -> Unit) = ElmScaffold(controller) {
    val isLoading by watchAsState { it.loaders > 0 }
    val tasks by watchAsState { it.tasks }
    val sortedTasks by watchAsState { it.sortedTasks }
    val selectedTasks by watchAsState { it.selectedTasks }
    val loaderBackground by animateColorAsState(
        targetValue = if (isLoading && tasks.isNotEmpty())
            ColorLoaderBackground
        else
            Color.Transparent,
        label = ""
    )
    val listPadding by animateDpAsState(targetValue = if (selectedTasks.isNotEmpty()) 60.dp else 0.dp, label = "")

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            TasksAppBar(
                title = stringResource(R.string.tasks_title),
                menuIcon = painterResource(R.drawable.ic_menu),
                menuIconClicked = { onMenuClick() },
                refreshIcon = painterResource(R.drawable.ic_reload_disabled),
                refreshIconClicked = { sendMessage(TasksMessages.msgRefresh()) }
            )
            Box {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = listPadding)
                ) {
                    item { SearchItem() }
                    items(sortedTasks) {
                        TaskItem(it.task, it.isTasksWithSameAddressPresented, it.isSelected)
                    }
                    if (isLoading && tasks.isEmpty())
                        item { LoaderItem() }
                }
                if (selectedTasks.isNotEmpty())
                    DeliveryButton(
                        text = stringResource(R.string.start_tasks).uppercase(),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                    ) { sendMessage(TasksMessages.msgStartClicked()) }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(loaderBackground)
                .gesturesDisabled(isLoading && tasks.isNotEmpty())
        ) {
            if (isLoading && tasks.isNotEmpty())
                LinearProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun ElmScaffoldContext<TasksContext, TasksState>.TaskItem(
    task: Task,
    isTasksWithSameAddressPresented: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isTasksWithSameAddressPresented) Color.Gray else Color.Transparent,
        label = ""
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(modifier = modifier
        .background(backgroundColor)
        .clickable(interactionSource = interactionSource, indication = null)
        { sendMessage(TasksMessages.msgTaskClicked(task)) }) {
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()

        ) {
            HtmlText(
                html = task.displayName,
                textSize = 16f,
                gravity = Gravity.CENTER,
                typeface = Typeface.create(Typeface.SANS_SERIF, 500, false),
                modifier = Modifier.weight(1f)
            )
            if (task.state.state != TaskState.CREATED)
                Icon(
                    painter = painterResource(R.drawable.ic_check_enabled),
                    contentDescription = null,
                    tint = if (task.state.byOtherUser)
                        Color.Green
                    else
                        Color.Unspecified,
                    modifier = Modifier
                )
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = if (isSelected)
                    painterResource(R.drawable.ic_chain_enabled)
                else
                    painterResource(R.drawable.ic_chain_disabled),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(40.dp)
                    .padding(2.dp)
                    .clickable(interactionSource = interactionSource, indication = null)
                    { sendMessage(TasksMessages.msgTaskSelectClick(task)) }
            )
        }
        Spacer(Modifier.height(8.dp))
    }

}

@Composable
private fun ElmScaffoldContext<TasksContext, TasksState>.SearchItem(modifier: Modifier = Modifier) {
    val search by watchAsState { it.searchFilter }
    var searchInput by remember { mutableStateOf(search) }

    LaunchedEffect(searchInput) {
        sendMessage(TasksMessages.msgSearch(searchInput))
    }
    SearchTextField(
        value = searchInput,
        onValueChange = { searchInput = it },
        onClearClicked = { searchInput = "" },
        modifier = modifier.padding(horizontal = 12.dp)
    )

}
