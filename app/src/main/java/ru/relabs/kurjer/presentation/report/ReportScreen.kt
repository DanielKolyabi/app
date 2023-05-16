package ru.relabs.kurjer.presentation.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemState
import ru.relabs.kurjer.domain.models.address
import ru.relabs.kurjer.domain.models.id
import ru.relabs.kurjer.domain.models.state
import ru.relabs.kurjer.presentation.base.compose.ElmScaffold
import ru.relabs.kurjer.presentation.base.compose.ElmScaffoldContext
import ru.relabs.kurjer.presentation.base.compose.common.AppBarLoadableContainer
import ru.relabs.kurjer.presentation.base.compose.common.DeliveryButton
import ru.relabs.kurjer.presentation.base.compose.common.DescriptionTextField
import ru.relabs.kurjer.presentation.base.compose.common.HintContainer
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorButtonLight
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorButtonPink
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorEntranceCoupleEnabled
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorFuchsia
import ru.relabs.kurjer.presentation.base.tea.ElmController

@Composable
fun ReportScreen(controller: ElmController<ReportContext, ReportState>) = ElmScaffold(controller) {
    val isLoading by watchAsState { it.loaders > 0 }
    val title by watchAsState { it.tasks.firstOrNull()?.taskItem?.address?.name ?: "Неизвестно" }
    val tasks by watchAsState { state -> state.tasks.sortedBy { it.taskItem.state } }
    val textSizeStorage = remember { controller.context.textSizeStorage }
    val entrancesSize by watchAsState { it.entrancesInfo.size }

    AppBarLoadableContainer(
        isLoading = isLoading,
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
            HintContainer(hintText = "", textSizeStorage = textSizeStorage)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(entrancesSize) {
                    EntranceItem(it, Modifier.fillMaxWidth())
                }
            }
            LazyRow {
                item {
                    Icon(painter = painterResource(R.drawable.ic_entrance_photo), contentDescription = null)
                }
                items(1) {
                    PhotoItem()
                }
            }
            DescriptionTextField(value = "", onValueChange = {}, placeholder = "aboba")
            Buttons()
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
private fun ElmScaffoldContext<ReportContext, ReportState>.EntranceItem(idx: Int, modifier: Modifier = Modifier) {
    val entranceInfo by watchAsState { it.entrancesInfo[idx] }
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


    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(8.dp)) {
        Box(
            modifier = Modifier
                .then(
                    if (entranceInfo.coupleEnabled)
                        Modifier.background(
                            color = ColorEntranceCoupleEnabled,
                            shape = RoundedCornerShape(5.dp)
                        ) else
                        Modifier
                )
                .padding(1.dp)
        ) {
            Text(
                text = "${entranceInfo.entranceNumber.number}п-${apartmentsCount}",
                textAlign = TextAlign.Center,
                modifier = Modifier.width(55.dp)
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
        Icon(painter = painterResource(R.drawable.ic_entrance_photo), contentDescription = null)
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

@Composable
private fun ElmScaffoldContext<ReportContext, ReportState>.PhotoItem() {

}

@Composable
private fun ElmScaffoldContext<ReportContext, ReportState>.Buttons() {
    Row {
        DeliveryButton(text = "123") {

        }
        DeliveryButton(text = "123") {

        }
    }
}