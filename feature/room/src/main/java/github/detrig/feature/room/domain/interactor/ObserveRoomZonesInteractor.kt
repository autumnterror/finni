package github.detrig.feature.room.domain.interactor

import github.detrig.feature.room.domain.model.RoomData
import github.detrig.feature.room.domain.model.RoomZone
import github.detrig.feature.room.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class ObserveRoomZonesInteractor(
    private val repository: RoomRepository,
    private val resolveAccess: ResolveRoomZoneAccessInteractor,
) {
    operator fun invoke(): Flow<RoomData> = flow {
        repository.initialize()
        val definitions = repository.zones()
        emitAll(repository.observeProgress().map { progress ->
            RoomData(
                zones = definitions.map { RoomZone(it, resolveAccess(it, progress)) },
                progress = progress,
            )
        })
    }
}
