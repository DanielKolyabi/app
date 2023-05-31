package ru.relabs.kurjer.presentation.addresses

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskDeliveryType
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemState
import ru.relabs.kurjer.domain.models.address
import ru.relabs.kurjer.domain.models.bypass
import ru.relabs.kurjer.domain.models.state
import ru.relabs.kurjer.domain.models.subarea
import ru.relabs.kurjer.presentation.base.compose.ElmScaffold
import ru.relabs.kurjer.presentation.base.compose.ElmScaffoldContext
import ru.relabs.kurjer.presentation.base.compose.common.AppBarLoadableContainer
import ru.relabs.kurjer.presentation.base.compose.common.DeliveryButton
import ru.relabs.kurjer.presentation.base.compose.common.LoaderItem
import ru.relabs.kurjer.presentation.base.compose.common.SearchTextField
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorButtonLight
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorFuchsia
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorTextDisabled
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorYellow
import ru.relabs.kurjer.presentation.base.tea.ElmController
import kotlin.math.max
import kotlin.math.min

@Composable
fun AddressesScreen(controller: ElmController<AddressesContext, AddressesState>) = ElmScaffold(controller) {
    val isLoading by watchAsState { it.loaders > 0 }
    val tasks by watchAsState { it.tasks }
    val sortedTasks by watchAsState { it.sortedTasks }
    val selectedAddress by watchAsState { it.selectedListAddress }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val targetScrollItem by remember {
        derivedStateOf {
            if (listState.layoutInfo.totalItemsCount == 0 || selectedAddress == null) return@derivedStateOf null
            val singleItems = listState.layoutInfo.totalItemsCount - sortedTasks.size

            selectedAddress?.let { address ->
                sortedTasks.indexOfFirst { it is AddressesItem.GroupHeader && it.subItems.firstOrNull()?.address?.id == address.id }
                    .takeIf { idx -> idx >= 0 }?.let { idx ->
                        val itemsOnScreen = listState.layoutInfo.visibleItemsInfo.lastIndex - listState.firstVisibleItemIndex
                        val visibleMiddleIdx = listState.firstVisibleItemIndex + itemsOnScreen / 2
                        val preferredIdx = when {
                            idx > visibleMiddleIdx -> min(idx + itemsOnScreen / 2, listState.layoutInfo.totalItemsCount)
                            idx < visibleMiddleIdx -> max(0, idx - itemsOnScreen / 2)
                            else -> visibleMiddleIdx
                        }
                        return@derivedStateOf preferredIdx + singleItems
                    }
            }
            null
        }
    }
    var flashingItemIdx: Int? by remember { mutableStateOf(null) }


    LaunchedEffect(targetScrollItem) {
        targetScrollItem?.let {
            listState.animateScrollToItem(it)
            flashingItemIdx = selectedAddress?.let { address ->
                sortedTasks.indexOfFirst { it is AddressesItem.GroupHeader && it.subItems.firstOrNull()?.address?.id == address.id }
            }
            scope.launch {
                delay(2000)
                flashingItemIdx = null
            }
        }
    }
    AppBarLoadableContainer(
        isLoading = isLoading,
        painterId = R.drawable.ic_back_new,
        title = stringResource(R.string.addresses_title),
        onBackClicked = { sendMessage(AddressesMessages.msgNavigateBack()) }
    ) {
        Box {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (tasks.size == 1)
                    item { SortingItem() }
                if (tasks.isNotEmpty())
                    item { SearchItem() }
                if (isLoading && tasks.isEmpty())
                    item { LoaderItem() }
                if (tasks.isNotEmpty() && tasks.any { it.deliveryType == TaskDeliveryType.Address })
                    item { StorageItem() }
                itemsIndexed(sortedTasks) { idx, it ->
                    when (it) {
                        is AddressesItem.GroupHeader -> HeaderItem(it.subItems, it.showBypass, flashingItemIdx == idx)
                        is AddressesItem.AddressItem -> TaskItem(it.task, it.taskItem)
                        is AddressesItem.FirmItem -> TaskItem(it.task, it.taskItem)
                        else -> {}
                    }
                }
            }
            DeliveryButton(
                text = stringResource(R.string.show_task_on_map).uppercase(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) { sendMessage(AddressesMessages.msgGlobalMapClicked()) }
        }
    }
}

@Composable
private fun ElmScaffoldContext<AddressesContext, AddressesState>.HeaderItem(
    items: List<TaskItem>,
    showBypass: Boolean,
    flashing: Boolean,
    modifier: Modifier = Modifier
) {
    val noneCreated = items.none { it.state == TaskItemState.CREATED }
    val backgroundColor by animateColorAsState(
        targetValue = if (flashing) ColorFuchsia else ColorFuchsia.copy(0f),
        animationSpec = if (flashing)
            InfiniteRepeatableSpec(
                animation = TweenSpec(500),
                repeatMode = RepeatMode.Reverse
            )
        else
            tween(500)
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        Divider(color = Color.DarkGray)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            val address = if (showBypass) {
                "${items.firstOrNull()?.subarea ?: "?"}-${items.firstOrNull()?.bypass ?: "?"} "
            } else {
                ""
            } + (items.firstOrNull()?.address?.name
                ?: stringResource(R.string.address_unknown))
            Text(
                text = address,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = if (noneCreated) ColorTextDisabled else Color.Black,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_placeholder),
                tint = if (noneCreated) Color.Unspecified.copy(0.4f) else Color.Unspecified,
                contentDescription = null, modifier = Modifier
                    .size(32.dp)
                    .then(
                        if (!noneCreated)
                            Modifier.clickable { sendMessage(AddressesMessages.msgAddressMapClicked(items)) }
                        else
                            Modifier
                    )
            )
        }
    }
}

@Composable
private fun ElmScaffoldContext<AddressesContext, AddressesState>.TaskItem(
    task: Task,
    taskItem: TaskItem,
    modifier: Modifier = Modifier
) {
    val needPhoto = when (taskItem) {
        is TaskItem.Common -> {
            taskItem.needPhoto || taskItem.entrancesData.any { it.photoRequired }
        }
        is TaskItem.Firm -> {
            taskItem.needPhoto
        }
    }
    val state = taskItem.state

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
    ) {
        Button(
            onClick = { sendMessage(AddressesMessages.msgTaskItemClicked(taskItem, task)) },
            colors = ButtonDefaults.buttonColors(backgroundColor = ColorButtonLight),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = when (taskItem) {
                    is TaskItem.Common -> {
                        "${task.name} №${task.edition}, ${taskItem.copies}экз."
                    }
                    is TaskItem.Firm -> {
                        listOf(
                            task.name,
                            "№${task.edition}",
                            "${taskItem.copies}экз.",
                            taskItem.firmName,
                            taskItem.office
                        )
                            .filter { it.isNotEmpty() }
                            .joinToString(", ")
                    }
                },
                color = if (needPhoto) {
                    when (state) {
                        TaskItemState.CLOSED -> ColorFuchsia.copy(alpha = 0.5f)
                        TaskItemState.CREATED -> ColorFuchsia
                    }
                } else {
                    when (state) {
                        TaskItemState.CLOSED -> Color.Black.copy(alpha = 0.4f)
                        TaskItemState.CREATED -> Color.Black
                    }
                }
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.ic_map),
            contentDescription = null,
            tint = when (state) {
                TaskItemState.CLOSED -> Color.Unspecified.copy(0.4f)
                TaskItemState.CREATED -> Color.Unspecified
            },
            modifier = Modifier
                .size(32.dp)
                .then(
                    when (state) {
                        TaskItemState.CLOSED -> {
                            Modifier
                        }
                        TaskItemState.CREATED -> {
                            Modifier.clickable { sendMessage(AddressesMessages.msgTaskItemMapClicked(task)) }
                        }
                    }
                )
        )
    }
}

@Composable
private fun ElmScaffoldContext<AddressesContext, AddressesState>.SortingItem(modifier: Modifier = Modifier) {
    val sortingMethod by watchAsState { it.sorting }

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Button(
            onClick = {
                if (sortingMethod != AddressesSortingMethod.STANDARD)
                    sendMessage(AddressesMessages.msgSortingChanged(AddressesSortingMethod.STANDARD))
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = when (sortingMethod) {
                    AddressesSortingMethod.STANDARD -> ColorFuchsia
                    AddressesSortingMethod.ALPHABETIC -> ColorButtonLight
                }
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .height(36.dp)
        ) {
            Text(text = stringResource(R.string.sort_standart_button), fontSize = 10.sp, color = Color.Black)
        }
        Button(
            onClick = {
                if (sortingMethod != AddressesSortingMethod.ALPHABETIC)
                    sendMessage(AddressesMessages.msgSortingChanged(AddressesSortingMethod.ALPHABETIC))
            },
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = when (sortingMethod) {
                    AddressesSortingMethod.STANDARD -> ColorButtonLight
                    AddressesSortingMethod.ALPHABETIC -> ColorFuchsia
                }
            ),
            modifier = Modifier
                .height(36.dp)
        ) {
            Text(text = stringResource(R.string.sort_alphabetic_button), fontSize = 10.sp, color = Color.Black)
        }
    }
}

@Composable
private fun OtherAddressesItem(modifier: Modifier = Modifier) {

}

@Composable
private fun ElmScaffoldContext<AddressesContext, AddressesState>.SearchItem(modifier: Modifier = Modifier) {
    val searchText by watchAsState { it.searchFilter }
    var searchTextInput by remember { mutableStateOf(searchText) }

    LaunchedEffect(searchTextInput) {
        sendMessage(AddressesMessages.msgSearch(searchTextInput))
    }
    SearchTextField(
        value = searchTextInput,
        onValueChange = { searchTextInput = it },
        onClearClicked = { searchTextInput = "" },
        modifier = modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun ElmScaffoldContext<AddressesContext, AddressesState>.StorageItem(modifier: Modifier = Modifier) {
    Button(
        onClick = { sendMessage(AddressesMessages.msgStorageClicked()) },
        shape = RoundedCornerShape(2.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = ColorYellow),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(text = stringResource(R.string.btn_storage_check).uppercase(), color = Color.Black)
    }
}


