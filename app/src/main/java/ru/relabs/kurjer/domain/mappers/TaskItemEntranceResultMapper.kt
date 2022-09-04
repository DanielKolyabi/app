package ru.relabs.kurjer.domain.mappers

import ru.relabs.kurjer.data.database.entities.TaskItemResultEntranceEntity
import ru.relabs.kurjer.domain.models.*

object TaskItemEntranceResultMapper {
    fun fromEntity(entity: TaskItemResultEntranceEntity): TaskItemEntranceResult = TaskItemEntranceResult(
        id = TaskItemEntranceId(entity.id),
        taskItemResultId = TaskItemResultId(entity.taskItemResultId),
        entranceNumber = EntranceNumber(entity.entrance),
        selection = ReportEntranceSelectionMapper.fromBits(entity.state),
        userDescription = entity.userDescription,
        isPhotoRequired = entity.isPhotoRequired
    )

    fun fromModel(model: TaskItemEntranceResult): TaskItemResultEntranceEntity = TaskItemResultEntranceEntity(
        id = model.id.id,
        taskItemResultId = model.taskItemResultId.id,
        entrance = model.entranceNumber.number,
        state = ReportEntranceSelectionMapper.toBits(model.selection),
        userDescription = model.userDescription,
        isPhotoRequired = model.isPhotoRequired
    )
}

object ReportEntranceSelectionMapper{
    fun fromBits(bits: Int) = ReportEntranceSelection(
        isEuro = bits and 0x0001 > 0,
        isWatch = bits and 0x0010 > 0,
        isStacked = bits and 0x0100 > 0,
        isRejected = bits and 0x1000 > 0
    )

    fun toBits(selection: ReportEntranceSelection): Int =
        takeBitIf(0x0001, selection.isEuro) or takeBitIf(0x0010, selection.isWatch) or
            takeBitIf(0x0100, selection.isStacked) or takeBitIf(0x1000, selection.isRejected)

    private fun takeBitIf(bit: Int, condition: Boolean): Int {
        return if (condition) {
            bit
        } else {
            0
        }
    }
}
