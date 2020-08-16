package ru.relabs.kurjer.presentation.base.tea

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

typealias ElmEffect<EffectCtx, State> = suspend ChannelWrapper<EffectCtx, State>.(EffectCtx, State) -> Unit
typealias ElmMessage<EffectCtx, State> = (State) -> Pair<State, List<ElmEffect<EffectCtx, State>>>
typealias RenderState<State> = (State) -> Unit
typealias RenderEquals<State> = (old: State, new: State) -> Boolean
typealias ElmRender<State> = Pair<RenderEquals<State>, RenderState<State>>

fun <EffectCtx, State> msgState(update: (State) -> State): ElmMessage<EffectCtx, State> =
    { update(it) to emptyList() }

fun <EffectCtx, State> msgEffects(
    update: (State) -> State,
    effects: (State) -> List<ElmEffect<EffectCtx, State>>
): ElmMessage<EffectCtx, State> = {
    val updated = update(it)
    updated to effects(updated)
}

fun <EffectCtx, State> msgEffect(effect: ElmEffect<EffectCtx, State>) =
    msgEffects<EffectCtx, State>({ it }, { listOf(effect) })

fun <EffectCtx, State> msgEmpty(): ElmMessage<EffectCtx, State> = msgState { it }

fun <State> render(
    sendIf: RenderEquals<State>,
    render: RenderState<State>
): ElmRender<State> = sendIf to render

fun <S, R> renderT(
    transform: (S) -> R,
    render: RenderState<R>,
    sendIf: RenderEquals<R> = { o, n -> o != n }
): ElmRender<S> = render(
    { o, n -> sendIf(transform(o), transform(n)) },
    { s -> render(transform(s)) }
)

interface ChannelWrapper<Ctx, State> {
    val messages: SendChannel<ElmMessage<Ctx, State>>
}

fun <S> rendersCollector(renders: List<ElmRender<S>>): suspend (S) -> Unit {
    var oldState: S? = null
    return { s ->
        when (val o = oldState) {
            null -> renders.forEach { (_, f) -> withContext(Dispatchers.Main) { f(s) } }
            else -> renders.filter { (f, _) -> f(o, s) }.forEach { (_, f) -> withContext(Dispatchers.Main) { f(s) } }
        }
        oldState = s
    }
}

fun <S> debugCollector(log: (String) -> Unit): suspend (S) -> Unit =
    { withContext(Dispatchers.Default) { Unit } } //log("New state is $it") } }

//suspend fun <S> watchState(controller: ElmController<*, S>, watchers: List<suspend (S) -> Unit>) {
//    withContext(Dispatchers.Default) {
//        for (s in controller.states) watchers.forEach { w -> w(s) }
//    }
//}

suspend fun <EffectCtx, State> renderStateEffect(
    rs: List<ElmRender<State>>
): ElmEffect<EffectCtx, State> = { _, s -> withContext(Dispatchers.Main) { rs.forEach { (_, r) -> r(s) } } }

fun <EffectCtx, State> defaultController(state: State, effectCtx: EffectCtx): ElmController<EffectCtx, State> {
    return object : ElmController<EffectCtx, State>(state, effectCtx) {
    }
}

fun <C, S> CoroutineScope.sendMessage(controller: ElmController<C, S>, message: ElmMessage<C, S>) {
    launch { controller.messages.send(message) }
}

@Suppress("EXPERIMENTAL_API_USAGE")
abstract class ElmController<EffectCtx, State>(default: State, val context: EffectCtx) {

    protected val supervisor = SupervisorJob()
    protected val scope = CoroutineScope(Dispatchers.Default + supervisor)

    private var state: State = default
    private val _messages = Channel<ElmMessage<EffectCtx, State>>(Channel.UNLIMITED)
    private val _states = BroadcastChannel<State>(Channel.BUFFERED)

    val messages: SendChannel<ElmMessage<EffectCtx, State>> get() = _messages

    fun start(init: ElmMessage<EffectCtx, State>) {
        scope.launch {
            launch { messages.send(init) }
            for (msg in _messages) {
                val (new, effects) = msg(state)
                if (new !== state) {
                    state = new
                    _states.send(new)
                }
                effects.forEach { scope.launch { it(wrapContext(_messages), context, state) } }
            }
        }
    }

    fun stateFlow(): Flow<State> = callbackFlow {
        val updates = _states.openSubscription()
        invokeOnClose { updates.cancel() }
        send(state)
        for (update in updates) send(update)
    }

    fun stop() {
        supervisor.cancelChildren()
    }

    private fun wrapContext(channel: SendChannel<ElmMessage<EffectCtx, State>>) =
        object : ChannelWrapper<EffectCtx, State> {
            override val messages: SendChannel<ElmMessage<EffectCtx, State>> get() = channel
        }

}