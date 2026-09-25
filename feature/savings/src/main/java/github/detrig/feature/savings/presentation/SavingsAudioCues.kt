package github.detrig.feature.savings.presentation

import github.detrig.core.audio.AudioCue

internal object SavingsAudioCues {
    val GoalSaved = AudioCue("savings.goal", "savings", "audio/kenney/confirmation_001.ogg",
        volume = 0.25f, priority = 2, blockMillis = 330)
    val Deposit = AudioCue("savings.deposit", "savings", "audio/kenney/drop_001.ogg",
        volume = 0.30f, priority = 2, blockMillis = 320)
    val Withdrawal = AudioCue("savings.withdraw", "savings", "audio/kenney/click_001.ogg",
        volume = 0.20f, priority = 1, blockMillis = 200)
}
