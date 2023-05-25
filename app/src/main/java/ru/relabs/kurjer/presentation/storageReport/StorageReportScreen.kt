package ru.relabs.kurjer.presentation.storageReport

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.StorageClosure
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.storage.StorageReportPhoto
import ru.relabs.kurjer.presentation.base.compose.ElmScaffold
import ru.relabs.kurjer.presentation.base.compose.ElmScaffoldContext
import ru.relabs.kurjer.presentation.base.compose.common.AppBarLoadableContainer
import ru.relabs.kurjer.presentation.base.compose.common.DeliveryButton
import ru.relabs.kurjer.presentation.base.compose.common.DescriptionTextField
import ru.relabs.kurjer.presentation.base.compose.common.HintContainer
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorFuchsia
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorHasPhoto
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorRequiredPhoto
import ru.relabs.kurjer.presentation.base.compose.common.themes.ColorSeparator
import ru.relabs.kurjer.presentation.base.tea.ElmController
import ru.relabs.kurjer.uiOld.helpers.formattedTimeDate

@Composable
fun StorageReportScreen(controller: ElmController<StorageReportContext, StorageReportState>) = ElmScaffold(controller) {
    val isLoading by watchAsState { it.loaders > 0 }
    val gpsLoading by watchAsState { it.isGPSLoading }
    val title by watchAsState { it.tasks.firstOrNull()?.storage?.address ?: "" }
    val storageCloseFirstRequired by watchAsState { it.tasks.firstOrNull()?.storageCloseFirstRequired == true }
    val hintText by watchAsState { it.tasks.firstOrNull()?.storage?.description.toString() }
    val closureList by watchAsState { it.closureList }

    AppBarLoadableContainer(
        isLoading = isLoading,
        gpsLoading = gpsLoading && isLoading,
        painterId = R.drawable.ic_back_new,
        title = title,
        titleColor = if (storageCloseFirstRequired) ColorFuchsia else Color.White,
        onBackClicked = { sendMessage(StorageReportMessages.msgNavigateBack()) }) {
        Column(modifier = Modifier.fillMaxSize()) {
            HintContainer(hintText = hintText, textSizeStorage = controller.context.textSizeStorage)
            LazyColumn (modifier = Modifier.fillMaxWidth().weight(1f)){
                itemsIndexed(closureList) { idx, closureInfo ->
                    ClosureItem(index = idx, task = closureInfo.task, closure = closureInfo.closure)
                }
            }
            DescriptionInput()
            PhotosRow()
            DeliveryButton(
                text = stringResource(R.string.show_task_on_map).uppercase(),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                sendMessage(StorageReportMessages.msgMapClicked())
            }
            DeliveryButton(
                text = stringResource(R.string.confirm_storage).uppercase(),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                sendMessage(StorageReportMessages.msgCloseClicked())
            }
        }

    }
}

@Composable
private fun ClosureItem(
    index: Int,
    task: Task,
    closure: StorageClosure,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    )
    {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.closure_date, index + 1, closure.closeDate.formattedTimeDate()),
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = task.listName,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = ColorSeparator)
    }
}

@Composable
private fun ElmScaffoldContext<StorageReportContext, StorageReportState>.DescriptionInput() {
    var descriptionInput by remember { mutableStateOf(stateSnapshot { it.storageReport?.description ?: "" }) }

    LaunchedEffect(descriptionInput) { sendMessage(StorageReportMessages.msgDescriptionChanged(descriptionInput)) }
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
private fun ElmScaffoldContext<StorageReportContext, StorageReportState>.PhotosRow(modifier: Modifier = Modifier) {
    val photos by watchAsState { it.storagePhotos }
    val tasks by watchAsState { it.tasks }
    val interactionSource = remember { MutableInteractionSource() }

    LazyRow(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        item {
            Icon(
                painter = painterResource(R.drawable.ic_entrance_photo),
                contentDescription = null,
                tint = if (tasks.any { it.storage.photoRequired })
                    ColorRequiredPhoto
                else if (photos.isNotEmpty())
                    ColorHasPhoto
                else
                    Color.Black,
                modifier = Modifier
                    .padding(8.dp)
                    .size(48.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { sendMessage(StorageReportMessages.msgPhotoClicked()) }
                    .padding(4.dp)
            )
        }
        items(photos) {
            PhotoItem(it.photo, it.uri)
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ElmScaffoldContext<StorageReportContext, StorageReportState>.PhotoItem(
    photo: StorageReportPhoto,
    uri: Uri,
    modifier: Modifier = Modifier
) {

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
                .clickable { sendMessage(StorageReportMessages.msgRemovePhotoClicked(photo)) }
                .padding(top = 2.dp, end = 2.dp)
                .size(24.dp)
        )
    }
}