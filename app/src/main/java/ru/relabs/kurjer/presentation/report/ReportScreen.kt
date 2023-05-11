package ru.relabs.kurjer.presentation.report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.address
import ru.relabs.kurjer.presentation.base.compose.ElmScaffold
import ru.relabs.kurjer.presentation.base.compose.common.AppBarLoadableContainer
import ru.relabs.kurjer.presentation.base.tea.ElmController

@Composable
fun ReportScreen(controller: ElmController<ReportContext, ReportState>) = ElmScaffold(controller) {
    val isLoading by watchAsState { it.loaders > 0 }
    val title by watchAsState { it.tasks.firstOrNull()?.taskItem?.address?.name ?: "Неизвестно" }

    AppBarLoadableContainer(
        isLoading = isLoading,
        painterId = R.drawable.ic_back_new,
        title = title,
        onBackClicked = { ReportMessages.msgBackClicked() }
    ) {

    }

}