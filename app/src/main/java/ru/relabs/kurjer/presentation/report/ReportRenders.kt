package ru.relabs.kurjer.presentation.report

import android.view.View
import ru.relabs.kurjer.presentation.base.tea.renderT
import ru.relabs.kurjer.utils.extensions.visible

/**
 * Created by Daniil Kurchanov on 06.04.2020.
 */
object ReportRenders {
    fun renderLoading(view: View): ReportRender = renderT(
        { it.loaders > 0 },
        { view.visible = it }
    )
}