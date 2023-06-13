package ru.relabs.kurjer.presentation.taskDetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskState
import ru.relabs.kurjer.domain.models.bypass
import ru.relabs.kurjer.domain.models.copies
import ru.relabs.kurjer.domain.models.needPhoto
import ru.relabs.kurjer.domain.models.subarea
import ru.relabs.kurjer.presentation.base.compose.ElmScaffold
import ru.relabs.kurjer.presentation.base.compose.ElmScaffoldContext
import ru.relabs.kurjer.presentation.base.compose.common.AppBarLoadableContainer
import ru.relabs.kurjer.presentation.base.compose.common.DeliveryButton
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorDivider
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorFuchsia
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorGrayBase
import ru.relabs.kurjer.presentation.base.tea.ElmController
import ru.relabs.kurjer.uiOld.helpers.formated


@Composable
fun TaskDetailsScreen(
    controller: ElmController<TaskDetailsContext, TaskDetailsState>
) = ElmScaffold(controller) {
    val isLoading by watchAsState { it.loaders > 0 }
    val listItems by watchAsState { it.sortedTasks }
    val task by watchAsState { it.task }
    val photoRequired by watchAsState { it.photoRequired }

    AppBarLoadableContainer(
        isLoading = isLoading,
        painterId = R.drawable.ic_back_new,
        title = stringResource(id = R.string.task_details_title),
        onBackClicked = { sendMessage(TaskDetailsMessages.msgNavigateBack()) },
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        task?.let {
                            PageHeader(
                                task = it,
                                photoRequired,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                            )
                        }
                    }
                    item { ListHeader() }
                    items(listItems.size) {
                        ListItem(
                            item = listItems[it],
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Buttons(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(alignment = Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun ListHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Divider(color = ColorDivider, thickness = 2.dp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.task_details_column_1),
                color = ColorGrayBase,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(42.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.task_details_column_2),
                textAlign = TextAlign.Center,
                color = ColorGrayBase,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.task_details_column_3),
                    color = ColorGrayBase,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.task_details_column_4),
                    color = ColorGrayBase,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(42.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PageHeader(task: Task, photoRequired: Boolean, modifier: Modifier = Modifier) {
    var maxWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Column(modifier = modifier) {
        Row {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Row {
                    Text(
                        text = stringResource(R.string.edition_label),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .onSizeChanged {
                                with(density) {
                                    if (it.width.toDp() > maxWidth)
                                        maxWidth = it.width.toDp()
                                }
                            }
                            .then(if (maxWidth.value != 0f) Modifier.width(maxWidth) else Modifier)
                    )
                    Text(text = stringResource(R.string.task_details_publisher, task.name, task.edition))
                }
                Spacer(Modifier.height(2.dp))
                Row {
                    Text(
                        text = stringResource(R.string.dates_label),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .onSizeChanged {
                                with(density) {
                                    if (it.width.toDp() > maxWidth)
                                        maxWidth = it.width.toDp()
                                }
                            }
                            .then(if (maxWidth.value != 0f) Modifier.width(maxWidth) else Modifier)
                    )
                    Text(
                        text = stringResource(
                            R.string.task_details_date_rande,
                            task.startTime.formated(),
                            task.endTime.formated()
                        )
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row {
                    Text(
                        text = stringResource(R.string.brigade_label),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .onSizeChanged {
                                with(density) {
                                    if (it.width.toDp() > maxWidth)
                                        maxWidth = it.width.toDp()
                                }
                            }
                            .then(if (maxWidth.value != 0f) Modifier.width(maxWidth) else Modifier)
                    )
                    Row {
                        Text(text = task.brigade.toString())
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.area_label),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = task.area.toString())
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.city_label),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = task.city)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row {
                    Text(
                        text = stringResource(R.string.prints_count_label),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .onSizeChanged {
                                with(density) {
                                    if (it.width.toDp() > maxWidth)
                                        maxWidth = it.width.toDp()
                                }
                            }
                            .then(if (maxWidth.value != 0f) Modifier.width(maxWidth) else Modifier)
                    )
                    Row {
                        Text(text = task.copies.toString())
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.city_label),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = task.packs.toString())
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.city_label),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = task.remain.toString())
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row {
                    Text(
                        text = stringResource(R.string.storage_label),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .onSizeChanged {
                                with(density) {
                                    if (it.width.toDp() > maxWidth)
                                        maxWidth = it.width.toDp()
                                }
                            }
                            .then(if (maxWidth.value != 0f) Modifier.width(maxWidth) else Modifier)
                    )
                    Text(text = task.storage.address)
                }
            }
        }
        if (photoRequired) {
            Row {
                Spacer(Modifier.width(40.dp))
                Text(
                    text = stringResource(R.string.task_details_photos_required),
                    color = ColorFuchsia
                )
            }
        }
    }
}

@Composable
private fun ElmScaffoldContext<TaskDetailsContext, TaskDetailsState>.ListItem(
    item: TaskItem,
    modifier: Modifier = Modifier
) {
    val photoRequired = item.needPhoto || (item is TaskItem.Common && item.entrancesData.any { it.photoRequired })

    Column(modifier = modifier) {
        Divider(color = ColorDivider, thickness = 2.dp)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.task_details_address_bypass, item.subarea, item.bypass),
                fontSize = 16.sp,
                color = ColorGrayBase,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = when (item) {
                    is TaskItem.Common -> item.address.name
                    is TaskItem.Firm -> listOf(item.address.name, item.firmName, item.office)
                        .filter { it.isNotEmpty() }
                        .joinToString(", ")
                },
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = if (photoRequired) ColorFuchsia else ColorGrayBase,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.copies.toString(),
                    fontSize = 16.sp,
                    color = ColorGrayBase,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    modifier = Modifier
                        .requiredSize(42.dp)
                        .padding(8.dp)
                        .clickable { sendMessage(TaskDetailsMessages.msgInfoClicked(item)) }

                )
            }
        }
    }
}


@Composable
private fun ElmScaffoldContext<TaskDetailsContext, TaskDetailsState>.Buttons(modifier: Modifier = Modifier) {
    val examinedButtonVisible by watchAsState { it.task?.state?.state == TaskState.CREATED }

    Row(modifier = modifier) {
        DeliveryButton(
            stringResource(R.string.show_map_button_text).uppercase(),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp, vertical = 12.dp)
        ) {
            sendMessage(TaskDetailsMessages.msgOpenMap())
        }
        if (examinedButtonVisible) {
            DeliveryButton(
                stringResource(R.string.examine_button_text).uppercase(),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 12.dp)
            ) {
                sendMessage(TaskDetailsMessages.msgExamineClicked())
            }
        }
    }
}