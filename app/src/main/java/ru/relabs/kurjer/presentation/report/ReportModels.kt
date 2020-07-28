package ru.relabs.kurjer.presentation.report

import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.presentation.base.tea.*

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

data class ReportState(
    val loaders: Int = 0
)

class ReportContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

}

typealias ReportMessage = ElmMessage<ReportContext, ReportState>
typealias ReportEffect = ElmEffect<ReportContext, ReportState>
typealias ReportRender = ElmRender<ReportState>