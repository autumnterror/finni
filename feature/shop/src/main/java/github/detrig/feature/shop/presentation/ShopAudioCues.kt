package github.detrig.feature.shop.presentation

import github.detrig.core.audio.AudioCue

internal object ShopAudioCues {
    val Select = AudioCue("shop.select", "shop", "audio/kenney/click_001.ogg",
        volume = 0.20f, priority = 0, cooldownMillis = 100, blockMillis = 160)
    val Rejected = AudioCue("shop.rejected", "shop", "audio/kenney/error_001.ogg",
        volume = 0.20f, priority = 2, cooldownMillis = 300, blockMillis = 400)
}
