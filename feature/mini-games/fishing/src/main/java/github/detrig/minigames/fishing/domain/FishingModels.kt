package github.detrig.minigames.fishing.domain

import kotlinx.serialization.Serializable

@Serializable internal enum class FishingPhase { COUNTDOWN, READY, CHARGING, FLYING, SEARCHING, FIGHTING, HAULING, CATCH_PENDING, RELEASING, JUNK_RELEASE, EMPTY_RETURN, BOMB_POP, FINISHED }
@Serializable internal enum class LossReason { NONE, TENSION, SLACK, EMPTY }
@Serializable internal enum class FinishReason { TIME, BOMB }
@Serializable internal enum class FishBehavior { SWIMMING, HOOKED }
@Serializable internal enum class ObjectKind { BOOT, CAN, BOMB }
@Serializable internal enum class FishingEventKind { CAST, BITE, SNAG, CATCH, OBJECT_DELIVERED, LOSS, FINISH }
@Serializable internal data class FishingEvent(val kind: FishingEventKind, val atSeconds: Double,
    val detail: String, val value: Double = 0.0)
@Serializable internal data class Hook(
    val x: Double = .34, val y: Double = -.15, val vx: Double = 0.0, val vy: Double = 0.0,
    val lineLength: Double = 2.7, val reelRemaining: Double = 0.0,
    val slowedDescent: Boolean = false,
)
@Serializable internal data class PondObject(
    val id: Int, val kind: ObjectKind, val x: Double, val y: Double,
    val visibleSeconds: Double = 0.0, val taken: Boolean = false,
)
@Serializable internal data class FishInstance(
    val spawnId: Int, val speciesId: String, val grams: Int, val respawnSeconds: Double = 0.0,
    val x: Double = .5, val y: Double = .4, val vx: Double = .04, val vy: Double = 0.0,
    val behavior: FishBehavior = FishBehavior.SWIMMING,
    val direction: Int = 1,
    val arrivalSeconds: Double = 0.0,
    val swimRng: Long = 1, val turnSeconds: Double = 4.0,
)
@Serializable internal data class FishingCatch(
    val id: String, val speciesId: String, val grams: Int, val caughtAtMillis: Long,
)
@Serializable internal data class FishingSession(
    val id: String,
    val profileId: String,
    val rulesVersion: Int,
    val rng: Long,
    val createdAtMillis: Long,
    val fish: List<FishInstance>,
    val nextSpawnId: Int,
    val tutorial: Boolean = false,
    val phase: FishingPhase = FishingPhase.COUNTDOWN,
    val phaseSeconds: Double = 0.0,
    val activeSeconds: Double = 0.0,
    val paused: Boolean = false,
    val resumeSeconds: Double = 0.0,
    val pulling: Boolean = false,
    val power: Double = 0.0,
    val castX: Double = 0.4,
    val targetSpawnId: Int? = null,
    val fightSeconds: Double = 0.0,
    val tension: Double = 0.0,
    val slackSeconds: Double = 0.0,
    val warning: Boolean = false,
    val tutorialWarningSeen: Boolean = false,
    val tutorialReleased: Boolean = false,
    val validCasts: Int = 0,
    val catches: List<FishingCatch> = emptyList(),
    val lastLoss: LossReason = LossReason.NONE,
    val bannerSeconds: Double = 0.0,
    val hook: Hook = Hook(),
    val objects: List<PondObject> = emptyList(),
    val attachedObjectId: Int? = null,
    val lastReelAt: Double = -10.0,
    val reelCount: Int = 0,
    val fightPullSeconds: Double = 0.0, val landingSpeed: Double = 0.0,
    val reelingSeconds: Double = 0.0,
    val cameraDepth: Double = 0.0,
    val simulationRemainder: Double = 0.0,
    val junkCount: Int = 0,
    val lossCount: Int = 0,
    val lostSpawnId: Int? = null,
    val finishReason: FinishReason = FinishReason.TIME,
    val bombSpawned: Boolean = false,
    val returnStart: Hook = Hook(),
    val returnDuration: Double = 1.2,
    val lastObjectKind: ObjectKind? = null,
    val recentEvents: List<FishingEvent> = emptyList(),
) {
    val target: FishInstance? get() = fish.firstOrNull { it.spawnId == targetSpawnId }
    val totalGrams: Int get() = catches.sumOf { it.grams }
    val attachedObject: PondObject? get() = objects.firstOrNull { it.id == attachedObjectId }
    val inWater get() = phase in setOf(FishingPhase.SEARCHING, FishingPhase.FIGHTING, FishingPhase.HAULING)
}
@Serializable internal data class FishingResult(
    val sessionId: String, val rulesVersion: Int, val grams: Int, val count: Int,
    val completedAtMillis: Long, val newRecord: Boolean, val eligibleForHappiness: Boolean,
    val previousRecordGrams: Int = 0, val recordGrams: Int = 0,
    val happinessDelta: Int? = null, val assisted: Boolean = false,
    val largestGrams: Int = 0, val junkCount: Int = 0, val lossCount: Int = 0,
    val reason: FinishReason = FinishReason.TIME,
)
@Serializable internal data class FishingRecord(
    val rulesVersion: Int, val grams: Int, val count: Int, val achievedAtMillis: Long,
    val assisted: Boolean = false,
)
@Serializable internal data class FishingPreferences(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val reducedMotion: Boolean = false,
)
@Serializable internal data class FishingProgress(
    val schemaVersion: Int = 5,
    val profileId: String,
    val tutorialDone: Boolean = false,
    val preferences: FishingPreferences = FishingPreferences(),
    val records: List<FishingRecord> = emptyList(),
    val session: FishingSession? = null,
    val lastResult: FishingResult? = null,
    val pendingEffects: List<FishingResult> = emptyList(),
    val migrationNotice: Boolean = false,
)
