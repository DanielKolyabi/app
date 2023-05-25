package ru.relabs.kurjer.presentation.report

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import kotlinx.coroutines.flow.StateFlow
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.ENTRANCE_NUMBER_TASK_ITEM
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemPhoto
import ru.relabs.kurjer.domain.models.TaskItemState
import ru.relabs.kurjer.domain.models.address
import ru.relabs.kurjer.domain.models.id
import ru.relabs.kurjer.domain.models.needPhoto
import ru.relabs.kurjer.domain.models.notes
import ru.relabs.kurjer.domain.models.state
import ru.relabs.kurjer.presentation.base.compose.ElmScaffold
import ru.relabs.kurjer.presentation.base.compose.ElmScaffoldContext
import ru.relabs.kurjer.presentation.base.compose.common.AppBarLoadableContainer
import ru.relabs.kurjer.presentation.base.compose.common.DeliveryButton
import ru.relabs.kurjer.presentation.base.compose.common.DescriptionTextField
import ru.relabs.kurjer.presentation.base.compose.common.HintContainer
import ru.relabs.kurjer.presentation.base.compose.common.gesturesDisabled
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorBackgroundGray
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorButtonLight
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorButtonPink
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorEntranceCoupleEnabled
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorFuchsia
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorHasPhoto
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorRequiredPhoto
import ru.relabs.kurjer.presentation.base.tea.ElmController

@Composable
fun ReportScreen(
    controller: ElmController<ReportContext, ReportState>,
    isCloseClickedFlow: StateFlow<Boolean>,
    onCloseButtonClicked: () -> Unit
) = ElmScaffold(controller) {
    val isLoading by watchAsState { it.loaders > 0 }
    val gpsLoading by watchAsState { it.isGPSLoading }
    val title by watchAsState { it.tasks.firstOrNull()?.taskItem?.address?.name ?: "Неизвестно" }
    val tasks by watchAsState { state -> state.tasks.sortedBy { it.taskItem.state } }
    val textSizeStorage = remember { controller.context.textSizeStorage }
    val entrancesInfo by watchAsState { it.entrancesInfo }
    val notes by watchAsState { it.selectedTask?.taskItem?.notes.orEmpty() }
    val firmAddress by watchAsState { it.firmAddress }
    val available by watchAsState { it.available }
    val isCloseClicked by isCloseClickedFlow.collectAsState()

    AppBarLoadableContainer(
        isLoading = isLoading,
        gpsLoading = isLoading && gpsLoading,
        painterId = R.drawable.ic_back_new,
        title = title,
        onBackClicked = { sendMessage(ReportMessages.msgBackClicked()) }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (tasks.size > 1)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items(tasks) { task ->
                        TaskItem(task)
                    }
                }
            HintContainer(
                hintText = (3 downTo 1).joinToString("<br/>") { notes.getOrElse(it - 1) { "" } },
                textSizeStorage = textSizeStorage
            )
            firmAddress?.let { Text(text = it, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)) }
            AvailableContainer(
                available = available,
                modifier = Modifier
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items(entrancesInfo) {
                        EntranceItem(it, Modifier.fillMaxWidth())
                    }
                }
            }
            AvailableContainer(available = available) {
                Column {
                    PhotosRow()
                    DescriptionInput()
                    Buttons(isCloseClicked, onCloseButtonClicked, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ElmScaffoldContext<ReportContext, ReportState>.DescriptionInput() {
    val report by watchAsState { it.selectedTaskReport }
    var descriptionInput by remember(report?.taskItemId) { mutableStateOf(report?.description ?: "") }
    val task by watchAsState { it.selectedTask }

    LaunchedEffect(descriptionInput, task) {
        task?.let { sendMessage(ReportMessages.msgDescriptionChanged(descriptionInput, it)) }
    }
    DescriptionTextField(
        value = descriptionInput,
        onValueChange = { descriptionInput = it },
        placeholder = stringResource(R.string.user_explanation_hint),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    )
}

@Composable
private fun ElmScaffoldContext<ReportContext, ReportState>.PhotosRow(modifier: Modifier = Modifier) {
    val photos by watchAsState { it.selectedTaskPhotos }
    val task by watchAsState { it.selectedTask }
    val interactionSource = remember { MutableInteractionSource() }

    LazyRow(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        item {
            Icon(
                painter = painterResource(R.drawable.ic_entrance_photo),
                contentDescription = null,
                tint = if (task?.taskItem?.needPhoto == true)
                    ColorRequiredPhoto
                else if (photos.any { it.photo.entranceNumber.number == ENTRANCE_NUMBER_TASK_ITEM })
                    ColorHasPhoto
                else
                    Color.Black,
                modifier = Modifier
                    .padding(8.dp)
                    .size(48.dp)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        sendMessage(
                            ReportMessages.msgPhotoClicked(
                                null,
                                false
                            )
                        )
                    }
                    .padding(4.dp)
            )
        }
        items(photos) {
            PhotoItem(it.photo, it.uri)
        }
    }
}

@Composable
private fun ElmScaffoldContext<ReportContext, ReportState>.TaskItem(taskWithItem: TaskWithItem, modifier: Modifier = Modifier) {
    val task = remember { taskWithItem.task }
    val taskItem = remember { taskWithItem.taskItem }
    val selected by watchAsState { taskWithItem == it.selectedTask }

    Button(
        contentPadding = PaddingValues(6.dp),
        onClick = { sendMessage(ReportMessages.msgTaskSelected(taskItem.id)) },
        shape = RoundedCornerShape(2.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = if (selected) ColorFuchsia else ColorButtonLight),
        modifier = modifier
            .widthIn(max = 200.dp)
            .padding(12.dp)
    ) {
        Text(
            text = when (taskItem) {
                is TaskItem.Common -> "${task.name} №${task.edition}, ${taskItem.copies}экз."
                is TaskItem.Firm -> "${task.name} №${task.edition}, ${taskItem.copies}экз., ${taskItem.firmName}, ${taskItem.office}"
            },
            textAlign = TextAlign.Center,
            color = when (taskItem.state) {
                TaskItemState.CREATED -> Color.Black
                TaskItemState.CLOSED -> Color.Black.copy(0.4f)
            }
        )
    }
}

@Composable
private fun ElmScaffoldContext<ReportContext, ReportState>.EntranceItem(entranceInfo: ReportEntranceItem, modifier: Modifier = Modifier) {
    val entranceData = remember { entranceInfo.taskItem.entrancesData.firstOrNull { it.number == entranceInfo.entranceNumber } }
    val apartmentsCount = remember { entranceData?.apartmentsCount ?: "?" }
    val euroActive = entranceInfo.selection.isEuro
    val watchActive = entranceInfo.selection.isWatch
    val stackedActive = entranceInfo.selection.isStacked
    val rejectedActive = entranceInfo.selection.isRejected
    val euroDefault = remember { entranceData?.isEuroBoxes ?: false }
    val watchDefault = remember { entranceData?.hasLookout ?: false }
    val stackedDefault = remember { entranceData?.isStacked ?: false }
    val rejectedDefault = remember { entranceData?.isRefused ?: false }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = modifier
            .height(IntrinsicSize.Min)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .then(
                    if (entranceInfo.coupleEnabled)
                        Modifier.background(
                            color = ColorEntranceCoupleEnabled,
                            shape = RoundedCornerShape(5.dp)
                        ) else
                        Modifier.background(
                            color = Color.White,
                            shape = RoundedCornerShape(5.dp)
                        )
                )
                .padding(2.dp)
                .clickable(interactionSource = interactionSource, indication = null) {
                    sendMessage(
                        ReportMessages.msgCoupleClicked(entranceInfo.entranceNumber)
                    )
                }
        ) {
            Text(
                text = "${entranceInfo.entranceNumber.number}п-${apartmentsCount}",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(55.dp)
                    .align(Alignment.Center)
            )
        }
        Button(
            contentPadding = PaddingValues(),
            colors = ButtonDefaults.buttonColors(backgroundColor = ColorButtonLight),
            shape = RoundedCornerShape(2.dp),
            onClick = { sendMessage(ReportMessages.msgEntranceDescriptionClicked(entranceInfo.entranceNumber)) },
            modifier = Modifier
                .width(32.dp)
                .height(40.dp)
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Text(text = "T", color = Color.Black)
        }

        EntranceButton(
            text = stringResource(R.string.euro),
            active = euroActive,
            hasDefault = euroDefault,
            onClick = { sendMessage(ReportMessages.msgEntranceSelectClicked(entranceInfo.entranceNumber, EntranceSelectionButton.Euro)) },
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 48.dp)
        )
        EntranceButton(
            text = stringResource(R.string.watch),
            active = watchActive,
            hasDefault = watchDefault,
            onClick = { sendMessage(ReportMessages.msgEntranceSelectClicked(entranceInfo.entranceNumber, EntranceSelectionButton.Watch)) },
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 58.dp)
        )
        EntranceButton(
            text = stringResource(R.string.pile),
            active = stackedActive,
            hasDefault = stackedDefault,
            onClick = { sendMessage(ReportMessages.msgEntranceSelectClicked(entranceInfo.entranceNumber, EntranceSelectionButton.Stack)) },
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 66.dp)
        )
        EntranceButton(
            text = stringResource(R.string.rejection),
            active = rejectedActive,
            hasDefault = rejectedDefault,
            onClick = { sendMessage(ReportMessages.msgEntranceSelectClicked(entranceInfo.entranceNumber, EntranceSelectionButton.Reject)) },
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 62.dp)
        )
        Icon(
            painter = if (entranceInfo.hasPhoto)
                painterResource(R.drawable.ic_entrance_photo_done)
            else if (entranceData?.photoRequired == true)
                painterResource(R.drawable.ic_entrance_photo_req)
            else
                painterResource(R.drawable.ic_entrance_photo),
            contentDescription = null,
            modifier = Modifier
                .clickable(interactionSource = interactionSource, indication = null) {
                    sendMessage(
                        ReportMessages.msgPhotoClicked(
                            entranceInfo.entranceNumber,
                            false
                        )
                    )
                }
                .height(40.dp)
                .padding(6.dp)
        )
    }
}

@Composable
private fun EntranceButton(text: String, active: Boolean, hasDefault: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        contentPadding = PaddingValues(vertical = 2.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (active)
                ColorFuchsia
            else if (hasDefault)
                ColorButtonPink
            else
                ColorButtonLight
        ),
        shape = RoundedCornerShape(2.dp),
        onClick = { onClick() },
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(text = text, fontSize = 12.sp, textAlign = TextAlign.Center, color = Color.Black)
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ElmScaffoldContext<ReportContext, ReportState>.PhotoItem(photo: TaskItemPhoto, uri: Uri, modifier: Modifier = Modifier) {

    Box(
        modifier = modifier
            .size(64.dp)
            .padding(8.dp)
    ) {
        GlideImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
        )
        Icon(
            painter = painterResource(R.drawable.ic_cancel_black_24dp),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable { sendMessage(ReportMessages.msgRemovePhotoClicked(photo)) }
                .padding(top = 2.dp, end = 2.dp)
                .size(24.dp)
        )
        Text(
            text = photo.entranceNumber.number.let { if (it == -1) "Д" else it.toString() },
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 3.dp, bottom = 2.dp)
                .size(20.dp)
                .background(color = ColorBackgroundGray, shape = CircleShape)
        )
    }
}

@Composable
private fun ElmScaffoldContext<ReportContext, ReportState>.Buttons(
    isCloseClicked: Boolean,
    onCloseButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFirm by watchAsState { it.selectedTask?.taskItem is TaskItem.Firm }

    Row(modifier) {
        DeliveryButton(text = stringResource(R.string.close_address_button).uppercase(), modifier = Modifier.weight(1f)) {
            if (!isCloseClicked) {
                sendMessage(ReportMessages.msgCloseClicked(null))
                onCloseButtonClicked()
            }
        }
        if (isFirm) {
            DeliveryButton(text = stringResource(R.string.reject_address_button).uppercase(), modifier = Modifier.weight(1f)) {
                sendMessage(ReportMessages.msgRejectClicked())
            }
        }
    }
}

@Composable
private fun AvailableContainer(available: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.gesturesDisabled(disabled = !available)) {
        content()
    }
}