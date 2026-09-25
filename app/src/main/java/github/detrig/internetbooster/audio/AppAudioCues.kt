package github.detrig.internetbooster.audio

import github.detrig.core.audio.AudioCue

internal object AppAudioCues {
    val Purchase = AudioCue(
        id = "shop.purchase", owner = "shop", assetPath = "audio/purchase.wav",
        volume = 0.28f, priority = 3, cooldownMillis = 500, blockMillis = 1_650,
    )
}
