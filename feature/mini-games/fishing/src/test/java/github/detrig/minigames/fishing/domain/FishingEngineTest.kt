package github.detrig.minigames.fishing.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

internal fun testConfig(): FishingConfig = Json { ignoreUnknownKeys = true }.decodeFromString(
    checkNotNull(FishingEngineTest::class.java.classLoader!!.getResource("fishing_balance.json")).readText())

class FishingEngineTest {
    private val engine = FishingEngine(testConfig().validate())
    private fun ready(tutorial: Boolean = false) = engine.create("round", "profile", 42, 1000, tutorial).copy(phase = FishingPhase.READY)
    private fun water() = ready().copy(phase = FishingPhase.SEARCHING, hook = Hook(.5, .4, lineLength = engine.config.world.maxLineLength), fish = emptyList(), objects = emptyList())
    private fun fight(species: String = "crucian"): FishingSession {
        val s = ready()
        val fish = s.fish.first { it.speciesId == species }.copy(x = .6, y = .5, direction = -1, behavior = FishBehavior.HOOKED)
        return s.copy(phase = FishingPhase.FIGHTING, targetSpawnId = fish.spawnId, fish = listOf(fish),
            hook = Hook(fish.x - engine.config.world.mouthOffset, fish.y), objects = emptyList(), lastReelAt = -1.0, tension = engine.config.fight.initialTension,
            landingSpeed = kotlin.math.hypot(fish.x - engine.config.world.mouthOffset - engine.config.world.rodX, fish.y - engine.config.world.landingDepth) / engine.requiredHoldSeconds(fish.grams))
    }
    private fun tap(s: FishingSession) = engine.release(engine.press(s))
    private fun advance(s: FishingSession, seconds: Double) = engine.advance(s, seconds)

    @Test fun countdownIsNotActiveTime() {
        val s = advance(engine.create("r", "p", 1, 1), engine.config.round.countdownSeconds)
        assertEquals(FishingPhase.READY, s.phase)
        assertEquals(0.0, s.activeSeconds, 0.00001)
    }
    @Test fun threeSecondResumeEndsExactlyAndUnblocksInputAtEveryFrameRate() {
        for (fps in listOf(30, 60, 120)) {
            val initial = water().copy(resumeSeconds = 3.0)
            var s = initial
            repeat(3 * fps) { s = advance(s, 1.0 / fps) }
            assertEquals(0.0, s.resumeSeconds, 0.0)
            assertEquals(initial.activeSeconds, s.activeSeconds, .00001)
            assertEquals(initial.hook, s.hook)
            assertEquals(1, tap(s).reelCount)
        }
    }
    @Test fun chargeIsLinearClampedAndDoesNotReserveFish() {
        val short = engine.release(advance(engine.press(ready()), .2))
        val long = engine.release(advance(engine.press(ready()), 1.2))
        val max = engine.release(advance(engine.press(ready()), 4.0))
        assertTrue(short.castX < long.castX && long.castX < max.castX)
        assertEquals(1.0, max.power, 0.0)
        assertNull(short.targetSpawnId)
        assertNull(long.targetSpawnId)
        assertEquals(FishingPhase.FLYING, short.phase)
    }
    @Test fun noFishMeansNoTimedBiteAndHookStopsAtBottom() {
        val s = advance(water(), 25.0)
        assertEquals(FishingPhase.SEARCHING, s.phase)
        assertNull(s.target)
        assertEquals(engine.config.world.maxDepth, s.hook.y, .0001)
    }
    @Test fun distantFishDoesNotTeleportToHook() {
        val fish = FishInstance(1, "catfish", 1800, x = .85, y = .82)
        val s = advance(water().copy(hook = Hook(.07, .1), fish = listOf(fish)), 1.0)
        assertEquals(FishingPhase.SEARCHING, s.phase)
        assertTrue(s.fish.single().x > .7)
    }
    @Test fun bodyCrossingHooksFishAndNearbyMissKeepsItsTrajectory() {
        val stillWater = FishingEngine(engine.config.copy(world = engine.config.world.copy(sinkSpeed = 0.0, currentSpeed = 0.0)))
        val fish = FishInstance(1, "crucian", 230, x = .58, y = .3, direction = -1)
        val initial = water().copy(fish = listOf(fish), hook = Hook(.5, .3))
        val beforeContact = stillWater.advance(initial, .1)
        assertEquals(FishingPhase.SEARCHING, beforeContact.phase)
        assertEquals(FishBehavior.SWIMMING, beforeContact.fish.single().behavior)
        val bitten = stillWater.advance(beforeContact, 1.0)
        assertEquals(FishingPhase.FIGHTING, bitten.phase)
        assertEquals(230, bitten.target!!.grams)
        assertTrue(kotlin.math.abs(stillWater.mouthX(bitten.target!!) - bitten.hook.x) < .02)
        val miss = initial.copy(hook = Hook(.5, .34))
        val missed = stillWater.advance(miss, 1.0)
        val noBait = stillWater.advance(miss.copy(phase = FishingPhase.READY), 1.0)
        assertEquals(FishingPhase.SEARCHING, missed.phase)
        assertEquals(noBait.fish, missed.fish)
    }
    @Test fun turnsAndDepthChangesAreIndependentOfHookAndStayInSpeciesBand() {
        val world = ready().copy(objects = emptyList())
        val hooks = world.copy(phase = FishingPhase.SEARCHING, hook = Hook(.05, 2.3))
        val without = advance(world, 12.0)
        val with = advance(hooks, 12.0)
        assertEquals(without.fish, with.fish)
        assertTrue(without.fish.zip(world.fish).any { (a, b) -> a.y != b.y })
        var s = world
        val directions = mutableSetOf<Int>()
        repeat(600) {
            s = advance(s, .1)
            directions += s.fish.first().direction
            s.fish.forEach { f ->
                val rule = engine.config.fish(f.speciesId)
                assertTrue(f.x in rule.worldXPatrol[0]..rule.worldXPatrol[1])
                assertTrue(f.y in rule.waterDepth[0]..rule.waterDepth[1])
            }
        }
        assertEquals(setOf(-1, 1), directions)
    }
    @Test fun tenFishLiveAtDifferentDepthsAndCameraFollowsEmptyHookThenReturns() {
        val initial = ready()
        assertEquals(10, initial.fish.size)
        assertTrue(initial.fish.count { it.y < .9 } <= 6)
        assertTrue(initial.fish.first { it.speciesId == "catfish" }.y > 1.9)
        val deep = advance(water(), 20.0)
        assertTrue(deep.hook.y > 2 && deep.cameraDepth > 1.3)
        assertEquals(deep, advance(engine.pause(deep), 5.0).copy(paused = false))
        val back = advance(engine.recast(deep), 2.0)
        assertEquals(FishingPhase.READY, back.phase)
        assertEquals(0.0, back.cameraDepth, .001)
    }
    @Test fun largeFishNeedLongerHoldingAndNearBoatCatchCannotSkipTheFight() {
        assertTrue(engine.requiredHoldSeconds(200) in 3.0..4.0)
        assertTrue(engine.requiredHoldSeconds(1800) in 7.0..8.0)
        val near = advance(engine.press(fight().copy(hook = Hook(engine.config.world.rodX, .1))), .5)
        assertEquals(FishingPhase.FIGHTING, near.phase)
        assertTrue(near.fightPullSeconds < engine.requiredHoldSeconds(near.target!!.grams))
    }

    @Test fun firstContactWinsAndSimultaneousContactUsesStableId() {
        val a = FishInstance(2, "crucian", 200, x = .5 - engine.config.world.mouthOffset, y = .3)
        val b = a.copy(spawnId = 1, grams = 300)
        val s = advance(water().copy(hook = Hook(.5, .3), fish = listOf(a, b)), .01)
        assertEquals(1, s.targetSpawnId)
        assertEquals(1, s.fish.count { it.behavior == FishBehavior.HOOKED })
        val obj = PondObject(10001, ObjectKind.CAN, .5, .4)
        val objectFirst = advance(water().copy(objects = listOf(obj), fish = listOf(a.copy(x = .68))), .01)
        assertEquals(FishingPhase.HAULING, objectFirst.phase)
    }
    @Test fun emptyHookOnlyLiftsAfterCompletedTap() {
        val held = advance(engine.press(water()), .5)
        assertEquals(0, held.reelCount)
        val tapped = engine.release(held)
        assertEquals(1, tapped.reelCount)
        val moved = advance(tapped, .1)
        assertTrue(moved.hook.y < tapped.hook.y)
        assertEquals(1, moved.reelCount)
    }
    @Test fun tapsCannotAccumulatePullingAndHoldingMovesFishContinuously() {
        val initial = fight()
        var tapped = initial
        repeat(100) { tapped = tap(tapped) }
        assertEquals(initial.hook, tapped.hook)
        assertEquals(0.0, tapped.fightPullSeconds, 0.0)
        val held = advance(engine.press(initial), .5)
        assertTrue(held.hook.y < initial.hook.y)
        assertTrue(held.tension > initial.tension)
        assertEquals(.5, held.fightPullSeconds, .00001)
        assertEquals(0.0, held.hook.reelRemaining, 0.0)
    }

    @Test fun releasingStopsPullingImmediatelyAndCoolsEvenDuringFishBurst() {
        val initial = fight("catfish").copy(tension = 70.0, fightSeconds = engine.config.fish("catfish").burst!!.firstAtSeconds)
        val held = advance(engine.press(initial), .1)
        assertTrue(engine.isBurst(held))
        val released = engine.release(held)
        val rested = advance(released, .5)
        assertFalse(rested.pulling)
        assertEquals(released.fightPullSeconds, rested.fightPullSeconds, 0.0)
        assertEquals(released.reelingSeconds, rested.reelingSeconds, 0.0)
        assertTrue(rested.hook.y >= released.hook.y - .001)
        assertTrue(rested.tension < released.tension - 15)
        val resumed = advance(engine.press(rested), .2)
        assertTrue(resumed.hook.y < rested.hook.y)
        assertTrue(resumed.fightPullSeconds > rested.fightPullSeconds)
    }

    @Test fun briefPauseCoolsLineButLongSlackLosesOnlyCurrentFish() {
        val catch = FishingCatch("previous", "roach", 150, 1)
        val start = fight().copy(catches = listOf(catch), tension = 80.0)
        val rested = advance(start, .7)
        assertEquals(FishingPhase.FIGHTING, rested.phase)
        assertTrue(rested.tension < start.tension)
        val lost = advance(rested, 2.9)
        assertEquals(LossReason.SLACK, lost.lastLoss)
        assertEquals(listOf(catch), lost.catches)
        assertEquals(1, lost.lossCount)
    }
    @Test fun fullTensionImmediatelyLosesFishEvenAtBoatAndKeepsEarlierCatch() {
        val catch = FishingCatch("old", "roach", 200, 1)
        for (tension in listOf(100.0, 99.99)) {
            val s = fight().copy(hook = Hook(engine.config.world.rodX, .06), pulling = true,
                tension = tension, fightPullSeconds = 8.0, catches = listOf(catch))
            val lost = advance(s, .01)
            assertEquals(FishingPhase.EMPTY_RETURN, lost.phase)
            assertEquals(LossReason.TENSION, lost.lastLoss)
            assertEquals(listOf(catch), lost.catches)
            assertNull(lost.target)
            assertFalse(lost.pulling)
        }
        val safe = advance(engine.press(fight().copy(tension = 99.0)), .01)
        assertEquals(FishingPhase.FIGHTING, safe.phase)
        assertEquals(FishingPhase.FIGHTING, advance(engine.release(safe), .5).phase)
        assertEquals(LossReason.TENSION, advance(engine.release(fight().copy(tension = 100.0)), .01).lastLoss)
    }

    @Test fun fishBitingDuringHoldStartsContinuousReelingWithoutAnotherPress() {
        val fish = FishInstance(1, "crucian", 200, x = .48, y = .3)
        val bitten = advance(engine.press(water().copy(fish = listOf(fish), hook = Hook(.5, .3))), .01)
        assertEquals(FishingPhase.FIGHTING, bitten.phase)
        assertTrue(bitten.pulling)
        val reeled = advance(bitten, .2)
        assertTrue(reeled.hook.y < bitten.hook.y)
        assertTrue(reeled.fightPullSeconds > 0)
    }

    @Test fun safeDeliveryCreatesOnePendingCatchAndResetsMeters() {
        val arrived = advance(fight().copy(hook = Hook(engine.config.world.rodX, .06),
            pulling = true, fightPullSeconds = 8.0, power = .8), .01)
        assertEquals(FishingPhase.CATCH_PENDING, arrived.phase)
        val confirmed = engine.confirmCatch(arrived, 10)
        assertEquals(1, confirmed.catches.size)
        assertNull(confirmed.target)
        assertEquals(0.0, confirmed.power, 0.0)
        assertEquals(0.0, confirmed.tension, 0.0)
    }

    @Test fun deadlineRejectsUndeliveredFishAndBombWithNoGrace() {
        for (s in listOf(fight(), water().copy(phase = FishingPhase.HAULING,
            attachedObjectId = 10003, objects = listOf(PondObject(10003, ObjectKind.BOMB, .5, .4, 2.0, true))))) {
            val ended = advance(s.copy(activeSeconds = 74.99), .02)
            assertEquals(FishingPhase.FINISHED, ended.phase)
            assertEquals(FinishReason.TIME, ended.finishReason)
            assertTrue(ended.catches.isEmpty())
            assertEquals(75.0, ended.activeSeconds, .000001)
        }
    }
    @Test fun deliveryStrictlyBeforeDeadlineCounts() {
        val s = fight().copy(activeSeconds = 74.99, lastReelAt = 74.99, reelCount = 1, fightPullSeconds = 8.0, pulling = true, landingSpeed = engine.config.world.impulseSpeed,
            hook = Hook(engine.config.world.rodX, .083, reelRemaining = .1))
        val pending = advance(s, .01)
        assertEquals(FishingPhase.CATCH_PENDING, pending.phase)
        assertTrue(pending.activeSeconds < 75)
    }
    @Test fun deliveryExactlyAtDeadlineDoesNotCount() {
        val w = engine.config.world
        val s = fight().copy(activeSeconds = 74.99, lastReelAt = 74.99, reelCount = 1, fightPullSeconds = 8.0, pulling = true, landingSpeed = engine.config.world.impulseSpeed,
            hook = Hook(w.rodX, w.landingDepth + w.landingRadius + w.impulseSpeed * .01, reelRemaining = .1))
        assertEquals(FishingPhase.FINISHED, advance(s, .01).phase)
    }
    @Test fun releasingAnimationConsumesTime() {
        val s = ready().copy(phase = FishingPhase.RELEASING, activeSeconds = 74.9)
        assertEquals(FishingPhase.FINISHED, advance(s, .2).phase)
    }
    @Test fun emptyReturnIsAnimatedAndDoesNotCatchAnything() {
        val s = engine.recast(water())
        assertEquals(FishingPhase.EMPTY_RETURN, s.phase)
        val halfway = advance(s, .5)
        assertTrue(halfway.hook.y < s.hook.y && halfway.hook.y > engine.config.world.rodDepth)
        assertEquals(FishingPhase.EMPTY_RETURN, halfway.phase)
        assertEquals(FishingPhase.READY, advance(halfway, .8).phase)
    }
    @Test fun junkSnagsOnlyEmptyHookAndScoresNoFish() {
        val obj = PondObject(10001, ObjectKind.BOOT, .5, .4)
        var s = advance(water().copy(objects = listOf(obj)), .01)
        assertEquals(FishingPhase.HAULING, s.phase)
        assertEquals(s, engine.recast(s))
        s = advance(engine.press(s), 3.0)
        assertEquals(1, s.junkCount)
        assertEquals(0, s.totalGrams)
        assertTrue(s.catches.isEmpty())
    }
    @Test fun bombHasVisibleGraceAndOnlyHookCollisionMatters() {
        val young = PondObject(10003, ObjectKind.BOMB, .5, .4, .1)
        val safe = advance(water().copy(objects = listOf(young)), .1)
        assertEquals(FishingPhase.SEARCHING, safe.phase)
        val bitten = advance(water().copy(objects = listOf(young.copy(visibleSeconds = 2.0))), .01)
        assertEquals(FishingPhase.HAULING, bitten.phase)
        val withFish = fight().copy(objects = listOf(young.copy(x = .5, y = .5, visibleSeconds = 2.0)))
        assertEquals(FishingPhase.FIGHTING, advance(withFish, .1).phase)
    }
    @Test fun bombDeliveryEndsRoundAndKeepsEarlierCatch() {
        val catch = FishingCatch("earlier", "crucian", 250, 1)
        val s = water().copy(phase = FishingPhase.HAULING, attachedObjectId = 10003,
            hook = Hook(engine.config.world.rodX, .06, reelRemaining = .1),
            pulling = true, reelCount = 1, lastReelAt = 0.0, catches = listOf(catch),
            objects = listOf(PondObject(10003, ObjectKind.BOMB, .34, .06, 2.0, true)))
        val pop = advance(s, .01)
        assertEquals(FishingPhase.BOMB_POP, pop.phase)
        val finished = advance(pop, 1.0)
        assertEquals(FishingPhase.FINISHED, finished.phase)
        assertEquals(FinishReason.BOMB, finished.finishReason)
        assertEquals(250, finished.totalGrams)
    }
    @Test fun bombDoesNotSpawnInFirstFifteenSecondsOrPractice() {
        assertTrue(advance(ready(), 15.0).objects.none { it.kind == ObjectKind.BOMB })
        assertEquals(1, advance(ready(), 18.0).objects.count { it.kind == ObjectKind.BOMB })
        assertTrue(advance(ready(true), 30.0).objects.isEmpty())
    }
    @Test fun pauseCancelsChargeFreezesEverythingAndDoesNotResumeInput() {
        val paused = engine.pause(advance(engine.press(ready()), .5))
        assertEquals(FishingPhase.READY, paused.phase)
        assertEquals(paused, advance(paused, 40.0))
        val resumed = engine.resume(paused)
        val countdown = advance(resumed, 1.0)
        assertEquals(paused.activeSeconds, countdown.activeSeconds, 0.0)
        assertFalse(countdown.pulling)
        assertEquals(0, countdown.validCasts)
    }
    @Test fun worldSurvivesSerializationAndFrameRateChanges() {
        val base = engine.create("r", "p", 31, 0)
        fun run(fps: Int): FishingSession {
            var s = base
            repeat(12 * fps) { s = advance(s, 1.0 / fps) }
            return s
        }
        val slow = run(30); val fast = run(120)
        assertEquals(slow.copy(simulationRemainder = 0.0), fast.copy(simulationRemainder = 0.0))
        val restored = Json.decodeFromString<FishingSession>(Json.encodeToString(slow))
        assertEquals(slow, restored)
        assertTrue(slow.catches.isEmpty())
    }
    @Test fun practiceRequiresVisibleWarningAndRelaxationBeforeLanding() {
        var s = fight().copy(tutorial = true)
        repeat(600) {
            if (s.phase == FishingPhase.FIGHTING) {
                s = if (!s.tutorialWarningSeen || s.tutorialReleased) engine.press(s) else engine.release(s)
            }
            s = advance(s, .1)
            if (s.phase == FishingPhase.CATCH_PENDING) return@repeat
        }
        assertTrue(s.tutorialWarningSeen)
        assertTrue(s.tutorialReleased)
        assertEquals(FishingPhase.CATCH_PENDING, s.phase)
    }

    @Test fun everySpeciesCanBeLandedByHoldingAndBriefRelaxation() {
        for (rule in engine.config.species) {
            var s = fight(rule.id)
            repeat(700) {
                if (s.phase == FishingPhase.FIGHTING) {
                    s = if (!s.warning && !engine.burstSoon(s) && !engine.isBurst(s)) engine.press(s) else engine.release(s)
                    s = advance(s, .05)
                }
            }
            assertTrue("Holding effort: " + rule.id, s.fightPullSeconds + .00001 >= engine.requiredHoldSeconds(s.target!!.grams))
            assertEquals("Species: " + rule.id + ", loss: " + s.lastLoss, FishingPhase.CATCH_PENDING, s.phase)
        }
    }



    @Test fun frontBodyAndSmallMarginCatchButTailAndDistantWaterDoNot() {
        val still = FishingEngine(engine.config.copy(world = engine.config.world.copy(sinkSpeed = 0.0, currentSpeed = 0.0, swimSpeed = 0.0)))
        for (direction in listOf(-1, 1)) {
            val fish = FishInstance(1, "crucian", 230, x = .5, y = .3, direction = direction)
            for ((dx, dy) in listOf(0.0 to 0.0, direction * .018 to .02, direction * .05 to 0.0)) {
                val s = still.advance(water().copy(fish = listOf(fish), hook = Hook(.5 + dx, .3 + dy)), .01)
                assertEquals("Body margin $direction $dx $dy", FishingPhase.FIGHTING, s.phase)
            }
            for ((dx, dy) in listOf(-direction * .04 to 0.0, 0.0 to .04, direction * .08 to 0.0)) {
                val initial = water().copy(fish = listOf(fish), hook = Hook(.5 + dx, .3 + dy))
                val s = still.advance(initial, .01)
                assertEquals(FishingPhase.SEARCHING, s.phase)
                assertEquals(still.advance(initial.copy(phase = FishingPhase.READY), .01).fish, s.fish)
            }
        }
    }

    @Test fun emptyTapLiftsVerticallyThenSinksSlowerPastTheOldLineLimit() {
        val initial = water().copy(hook = Hook(.6, .8, lineLength = 1.0))
        val lifted = advance(tap(initial), .1)
        assertEquals(initial.hook.x, lifted.hook.x, .00001)
        assertTrue(lifted.hook.y < initial.hook.y - .07)
        val sinking = advance(lifted, .5)
        assertTrue(sinking.hook.y > lifted.hook.y)
        assertTrue(sinking.hook.vy > 0 && sinking.hook.vy < engine.config.world.sinkSpeed)
        assertEquals(0.0, sinking.hook.reelRemaining, .00001)
        assertEquals(engine.config.world.maxDepth, advance(sinking, 30.0).hook.y, .00001)
        val restored = Json.decodeFromString<FishingSession>(Json.encodeToString(sinking))
        assertEquals(advance(sinking, 1.0), advance(restored, 1.0))
    }

    @Test fun newCastRestoresDescentSpeedAndCancelledTapDoesNotSlowIt() {
        val cancelled = engine.release(engine.press(water()), cancel = true)
        assertFalse(cancelled.hook.slowedDescent)
        val lifted = advance(tap(water()), .1)
        val ready = advance(engine.recast(lifted), 2.0)
        val cast = engine.release(advance(engine.press(ready), .5))
        val sinking = advance(cast, 1.0)
        assertEquals(FishingPhase.SEARCHING, sinking.phase)
        assertFalse(sinking.hook.slowedDescent)
        assertEquals(engine.config.world.sinkSpeed, sinking.hook.vy, .00001)
    }

    @Test fun repeatedLiftsAtSurfaceCannotLeaveBufferedImpulseOrStallHook() {
        var s = water().copy(hook = Hook(.6, .005))
        repeat(5) { s = advance(tap(s), .13) }
        val released = advance(s, 1.0)
        assertEquals(FishingPhase.SEARCHING, released.phase)
        assertEquals(0.0, released.hook.reelRemaining, .00001)
        assertTrue(released.hook.y > .04 && released.hook.vy > 0)
    }

    @Test fun lossAndEmptyReturnClearOldPowerAndTensionBeforeNextCast() {
        for (s in listOf(advance(engine.press(fight().copy(tension = 99.99, power = .8)), .01),
            engine.recast(water().copy(tension = 81.0, warning = true, power = .8)))) {
            val returned = advance(s, 2.0)
            assertEquals(FishingPhase.READY, returned.phase)
            assertEquals(0.0, returned.tension, 0.0)
            assertEquals(0.0, returned.power, 0.0)
            assertFalse(returned.warning)
            val cast = engine.release(advance(engine.press(returned), .2))
            assertEquals(.125, cast.power, .00001)
            assertEquals(0.0, cast.tension, 0.0)
        }
    }

    @Test fun heldFightIsFrameRateIndependentAndCancelledOrRestoredInputDoesNotPull() {
        val initial = engine.press(fight("catfish"))
        fun run(fps: Int): FishingSession {
            var s = initial
            repeat(fps) { s = advance(s, 1.0 / fps) }
            return s.copy(simulationRemainder = 0.0)
        }
        val s = run(30)
        assertEquals(s, run(120))
        val restored = Json.decodeFromString<FishingSession>(Json.encodeToString(s))
        assertEquals(advance(s, .1), advance(restored, .1))
        for (released in listOf(engine.release(s, cancel = true), engine.resume(engine.pause(restored)))) {
            assertFalse(released.pulling)
            assertEquals(s.fightPullSeconds, advance(released, .5).fightPullSeconds, 0.0)
        }
        val toggled = engine.toggle(fight())
        assertTrue(toggled.pulling)
        assertFalse(engine.toggle(toggled).pulling)
    }
}
