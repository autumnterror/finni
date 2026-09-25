package github.detrig.feature.fridge.presentation

import github.detrig.core.audio.AudioCue

internal enum class FeedingSound(val cue: AudioCue) {
    Pickup(AudioCue("feeding.pickup", "feeding", "feeding/audio/pickup.wav",
        volume = 0.28f, priority = 0, blockMillis = 140)),
    Bite(AudioCue("feeding.bite", "feeding", "feeding/audio/bite.wav",
        volume = 0.42f, priority = 2, blockMillis = 180)),
    Chew(AudioCue("feeding.chew", "feeding", "feeding/audio/chew.wav",
        volume = 0.32f, priority = 1, cooldownMillis = 180, blockMillis = 220)),
    Drink(AudioCue("feeding.drink", "feeding", "audio/drink_01.wav",
        volume = 0.30f, priority = 2, blockMillis = 550)),
}
