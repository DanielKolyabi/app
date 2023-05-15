package ru.relabs.kurjer.presentation.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorEntranceCoupleEnabled
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorFuchsia
import ru.relabs.kurjer.presentation.base.tea.ElmController

@Composable
fun ReportScreen(controller: ElmController<ReportContext, ReportState>) = ElmScaffold(controller) {
    val isLoading by watchAsState { it.loaders > 0 }
    val title by watchAsState { it.tasks.firstOrNull()?.taskItem?.address?.name ?: "Неизвестно" }
    val tasks by watchAsState { state -> state.tasks.sortedBy { it.taskItem.state } }
    val textSizeStorage = remember { controller.context.textSizeStorage }
    val entrancesInfo by watchAsState { it.entrancesInfo }


    AppBarLoadableContainer(
        isLoading = isLoading,
        painterId = R.drawable.ic_back_new,
        title = title,
        onBackClicked = { ReportMessages.msgBackClicked() }
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
                items(entrancesInfo) {
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
        onClick = { sendMessage(ReportMessages.msgTaskSelected(taskItem.id)) },
        shape = RoundedCornerShape(2.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = if (selected) ColorFuchsia else ColorButtonLight),
        modifier = modifier
            .widthIn(max = 200.dp)
            .padding(8.dp)
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
            colors = ButtonDefaults.buttonColors(backgroundColor = ColorButtonLight),
            onClick = { sendMessage(ReportMessages.msgEntranceDescriptionClicked(entranceInfo.entranceNumber)) },
            modifier = Modifier
                .width(32.dp)
                .height(40.dp)
        ) {
            Text(text = "T", color = if (entranceInfo.hasDescription) Color.Black else Color.Green)
        }
        Button(
            colors = ButtonDefaults.buttonColors(backgroundColor = ColorButtonLight),
            onClick = { sendMessage(ReportMessages.msgEntranceSelectClicked(entranceInfo.entranceNumber, EntranceSelectionButton.Euro)) },
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 48.dp)
        ) {
            Text(text = stringResource(R.string.euro), fontSize = 12.sp)
        }
        Button(
            colors = ButtonDefaults.buttonColors(backgroundColor = ColorButtonLight),
            onClick = { sendMessage(ReportMessages.msgEntranceSelectClicked(entranceInfo.entranceNumber, EntranceSelectionButton.Watch)) },
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 58.dp)
        ) {
            Text(text = stringResource(R.string.watch), fontSize = 12.sp)
        }
        Button(
            colors = ButtonDefaults.buttonColors(backgroundColor = ColorButtonLight),
            onClick = { sendMessage(ReportMessages.msgEntranceSelectClicked(entranceInfo.entranceNumber, EntranceSelectionButton.Stack)) },
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 66.dp)
        ) {
            Text(text = stringResource(R.string.pile), fontSize = 12.sp)
        }
        Button(
            colors = ButtonDefaults.buttonColors(backgroundColor = ColorButtonLight),
            onClick = { sendMessage(ReportMessages.msgEntranceSelectClicked(entranceInfo.entranceNumber, EntranceSelectionButton.Reject)) },
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 62.dp)
        ) {
            Text(text = stringResource(R.string.rejection), fontSize = 12.sp)
        }
        Icon(painter = painterResource(R.drawable.ic_entrance_photo), contentDescription = null)
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