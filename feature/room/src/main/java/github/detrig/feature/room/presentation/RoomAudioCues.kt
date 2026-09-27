package github.detrig.feature.room.presentation

import github.detrig.core.audio.AudioCue

internal object RoomAudioCues {
    val Sleep = AudioCue("room.sleep", "room", "audio/kenney/close_001.ogg",
        volume = 0.22f, priority = 2, blockMillis = 650)
    val HairDryer = AudioCue("room.bath.hair_dryer", "room.bath.hair_dryer",
        "audio/bathroom_hair_dryer.wav", volume = 0.32f, priority = 1,
        blockMillis = 4_800, looping = true)
    val SoapRubbing = AudioCue("room.bath.soap", "room.bath.soap",
        "audio/bathroom_soap_rubbing.wav", volume = 0.32f, priority = 1,
        blockMillis = 720, looping = true)
    val ShowerWater = AudioCue("room.bath.water", "room.bath.water",
        "audio/bathroom_shower_water.wav", volume = 0.32f, priority = 1,
        blockMillis = 1_200, looping = true)
}
