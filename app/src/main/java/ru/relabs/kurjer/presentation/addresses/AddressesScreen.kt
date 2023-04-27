package ru.relabs.kurjer.presentation.addresses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.TaskDeliveryType
import ru.relabs.kurjer.presentation.base.compose.ElmScaffold
import ru.relabs.kurjer.presentation.base.compose.ElmScaffoldContext
import ru.relabs.kurjer.presentation.base.compose.common.*
import ru.relabs.kurjer.presentation.base.tea.ElmController

@Composable
fun AddressesScreen(controller: ElmController<AddressesContext, AddressesState>) = ElmScaffold(controller) {
    val isLoading by watchAsState { it.loaders > 0 }
    val tasks by watchAsState { it.tasks }

    AppBarLoadableContainer(
        isLoading = isLoading,
        painterId = R.drawable.ic_back_new,
        title = stringResource(R.string.addresses_title),
        onBackClicked = { sendMessage(AddressesMessages.msgNavigateBack()) }
    ) {
        Box {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (tasks.size == 1)
                    item { SortingItem() }
                if (tasks.isNotEmpty())
                    item { SearchItem() }
                if (isLoading && tasks.isEmpty())
                    item { LoaderItem() }
                if (tasks.isNotEmpty() && tasks.any { it.deliveryType == TaskDeliveryType.Address })
                    item { StorageItem() }

            }
            DeliveryButton(
                text = stringResource(R.string.show_task_on_map),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) { sendMessage(AddressesMessages.msgGlobalMapClicked()) }
        }
    }
}

@Composable
private fun AddressItem(modifier: Modifier = Modifier) {

}

@Composable
private fun CommonTaskItem(modifier: Modifier = Modifier) {

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
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier
                .height(36.dp)
                .padding(vertical = 2.dp)
        ) {
            Text(text = stringResource(R.string.sort_standart_button), fontSize = 10.sp, color = Color.Black)
        }
        Button(
            onClick = {
                if (sortingMethod != AddressesSortingMethod.ALPHABETIC)
                    sendMessage(AddressesMessages.msgSortingChanged(AddressesSortingMethod.ALPHABETIC))
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = when (sortingMethod) {
                    AddressesSortingMethod.STANDARD -> ColorButtonLight
                    AddressesSortingMethod.ALPHABETIC -> ColorFuchsia
                }
            ),
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier
                .height(36.dp)
                .padding(vertical = 2.dp)
        ) {
            Text(text = stringResource(R.string.sort_alphabetic_button), fontSize = 10.sp, color = Color.Black)
        }
    }
}

@Composable
private fun BlankItem(modifier: Modifier = Modifier) {

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
        modifier = modifier
    )
}

@Composable
private fun StorageItem(modifier: Modifier = Modifier) {


}


