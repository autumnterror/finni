package github.detrig.feature.room.presentation

import github.detrig.core.audio.AudioCue

internal object RoomAudioCues {
    val Sleep = AudioCue("room.sleep", "room", "audio/kenney/close_001.ogg",
        volume = 0.22f, priority = 2, blockMillis = 650)
}
