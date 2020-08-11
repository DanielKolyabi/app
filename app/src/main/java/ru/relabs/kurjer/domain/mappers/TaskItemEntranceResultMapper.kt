package ru.relabs.kurjer.domain.mappers

import ru.relabs.kurjer.data.database.entities.TaskItemResultEntranceEntity
import ru.relabs.kurjer.domain.models.*

object TaskItemEntranceResultMapper {
    fun fromEntity(entity: TaskItemResultEntranceEntity): TaskItemEntranceResult = TaskItemEntranceResult(
        id = TaskItemEntranceId(entity.id),
        taskItemResultId = TaskItemResultId(entity.taskItemResultId),
        entranceNumber = EntranceNumber(entity.entrance),
        selection = ReportEntranceSelection(
            isEuro = entity.state and 0x0001 > 0,
            isWatch = entity.state and 0x0010 > 0,
            isStacked = entity.state and 0x0100 > 0,
            isRejected = entity.state and 0x1000 > 0
        )
    )

    fun fromModel(model: TaskItemEntranceResult): TaskItemResultEntranceEntity = TaskItemResultEntranceEntity(
        id = model.id.id,
        taskItemResultId = model.taskItemResultId.id,
        entrance = model.entranceNumber.number,
        state = takeBitIf(0x0001, model.selection.isEuro) or takeBitIf(0x0010, model.selection.isWatch) or
                takeBitIf(0x0100, model.selection.isStacked) or takeBitIf(0x1000, model.selection.isRejected)
    )

    private fun takeBitIf(bit: Int, condition: Boolean): Int {
        return if (condition) {
            bit
        } else {
            0
        }
    }
}
