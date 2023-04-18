package ru.relabs.kurjer.presentation.base.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.relabs.kurjer.presentation.base.tea.ElmController
import ru.relabs.kurjer.presentation.base.tea.ElmMessage
import ru.relabs.kurjer.presentation.base.tea.sendMessage

interface ElmScaffoldContext<CC, CS> {
    val scope: CoroutineScope
    val controller: ElmController<CC, CS>

    fun sendMessage(msg: ElmMessage<CC, CS>)
    fun trySendMessage(msg: ElmMessage<CC, CS>)

    @Composable
    fun <T> watchAsState(mapper: (CS) -> T): State<T>
    fun <T> stateSnapshot(mapper: (CS) -> T): T
}

class ElmScaffoldContextImpl<CC, CS>(
    override val controller: ElmController<CC, CS>,
    override val scope: CoroutineScope
) : ElmScaffoldContext<CC, CS> {
    override fun sendMessage(msg: ElmMessage<CC, CS>) {
        scope.sendMessage(controller, msg)
    }

    override fun trySendMessage(msg: ElmMessage<CC, CS>) {
        controller.messages.trySend(msg)
    }

    @Composable
    override fun <T> watchAsState(mapper: (CS) -> T): State<T> {
        return controller.mapCollectAsState(mapper)
    }

    override fun <T> stateSnapshot(mapper: (CS) -> T): T {
        return mapper(controller.currentState)
    }
}

@Composable
fun <CC, CS> ElmScaffold(
    controller: ElmController<CC, CS>,
    modifier: Modifier = Modifier,
    content: @Composable ElmScaffoldContext<CC, CS>.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = remember(scope) { ElmScaffoldContextImpl(controller, scope) }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        context.content()
    }
}

@Composable
fun <S, MS> ElmController<*, S>.mapCollectAsState(transform: (S) -> MS) = remember(this) {
    stateFlow()
        .map(transform)
        .distinctUntilChanged()
}.collectAsState(initial = transform(this.currentState))