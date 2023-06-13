package ru.relabs.kurjer.presentation.storageList

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.presentation.base.compose.ElmScaffold
import ru.relabs.kurjer.presentation.base.compose.ElmScaffoldContext
import ru.relabs.kurjer.presentation.base.compose.common.AppBarLoadableContainer
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorDivider
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorFuchsia
import ru.relabs.kurjer.presentation.base.tea.ElmController

@Composable
fun StorageListScreen(controller: ElmController<StorageListContext, StorageListState>) = ElmScaffold(controller) {
    val isLoading by watchAsState { it.loaders > 0 }
    val storageWithTasksList by watchAsState { it.storageWithTasksList }

    AppBarLoadableContainer(
        isLoading = isLoading,
        painterId = R.drawable.ic_back_new,
        title = stringResource(R.string.storage_addresses_title),
        onBackClicked = { sendMessage(StorageListMessages.msgNavigateBack()) }) {

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(storageWithTasksList) {
                StorageItem(storage = it.storage, tasks = it.tasks)
            }
        }
    }
}

@Composable
private fun ElmScaffoldContext<StorageListContext, StorageListState>.StorageItem(
    storage: Task.Storage,
    tasks: List<StorageListState.TaskWrapper>,
    modifier: Modifier = Modifier
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clickable { sendMessage(StorageListMessages.msgStorageItemClicked(tasks.map { it.task.id })) })
    {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = tasks.joinToString("\n") { it.task.storageListName },
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = storage.address,
            color = if (tasks.any { it.isStorageActuallyRequired })
                ColorFuchsia
            else
                Color.Black,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = ColorDivider)
    }
}