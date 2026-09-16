package github.detrig.minigames.fishing.domain

import kotlin.math.*

/** Мир одиночной рыбалки с независимыми маршрутами рыб. Внешних часов здесь нет. */
internal class FishingEngine(val config: FishingConfig) {
    private val w get() = config.world
    private val waterPhases = setOf(FishingPhase.SEARCHING, FishingPhase.FIGHTING, FishingPhase.HAULING)
    private val stopped = setOf(FishingPhase.CATCH_PENDING, FishingPhase.FINISHED)

    fun create(id: String, profileId: String, seed: Long, now: Long, tutorial: Boolean = false): FishingSession {
        var rng = seed and 0x7fffffffL
        val population = mutableListOf<FishInstance>()
        repeat(if (tutorial) 1 else w.populationSize) { index ->
            val rule = if (tutorial) config.fish(config.tutorial.forcedSpeciesId) else config.species[if (index < 6) index else (index - 6) % 4]
            rng = nextRandom(rng)
            val massSteps = (rule.massGrams.max - rule.massGrams.min) / config.population.massStepGrams + 1
            val grams = rule.massGrams.min + (rng % massSteps).toInt() * config.population.massStepGrams
            rng = nextRandom(rng)
            var x = rule.worldXPatrol[0] + randomUnit(rng) * (rule.worldXPatrol[1] - rule.worldXPatrol[0])
            var y = rule.waterDepth[0] + (index % 3 + .5) / 3 * (rule.waterDepth[1] - rule.waterDepth[0])
            var attempts = 0
            while ((population.any { hypot(it.x - x, it.y - y) < .095 } ||
                    hypot(x - .43, y - .61) < .1 || hypot(x - .79, y - .44) < .1) && attempts++ < 100) {
                rng = nextRandom(rng)
                x = rule.worldXPatrol[0] + randomUnit(rng) * (rule.worldXPatrol[1] - rule.worldXPatrol[0])
                rng = nextRandom(rng)
                y = rule.waterDepth[0] + randomUnit(rng) * (rule.waterDepth[1] - rule.waterDepth[0])
            }
            population += FishInstance(index + 1, rule.id, grams, x = x, y = y, vx = if (index % 2 == 0) .035 else -.04,
                direction = if (index % 2 == 0) 1 else -1, swimRng = nextRandom(rng),
                turnSeconds = w.turnMinSeconds + randomUnit(rng) * (w.turnMaxSeconds - w.turnMinSeconds))
        }
        val s = FishingSession(id, profileId, config.rulesVersion, rng, now, population, population.size + 1,
            tutorial = tutorial, phase = if (tutorial) FishingPhase.READY else FishingPhase.COUNTDOWN,
            hook = Hook(x = w.rodX, y = w.rodDepth, lineLength = w.maxLineLength),
            objects = if (tutorial) emptyList() else listOf(
                PondObject(10001, ObjectKind.BOOT, .43, .61), PondObject(10002, ObjectKind.CAN, .79, .44)))
        return s
    }
    fun rodX(@Suppress("UNUSED_PARAMETER") s: FishingSession) = w.rodX
    fun flightDuration(s: FishingSession) = w.flightMin + (w.flightMax - w.flightMin) * s.power
    fun fishX(@Suppress("UNUSED_PARAMETER") s: FishingSession, fish: FishInstance) = fish.x
    fun fishDepth(@Suppress("UNUSED_PARAMETER") s: FishingSession, fish: FishInstance) = fish.y
    fun castX(power: Double) = config.casting.minWorldX + (config.casting.maxWorldX - config.casting.minWorldX) * power.coerceIn(0.0, 1.0)
    fun splashX(s: FishingSession) = castX(s.power)
    fun mouthX(fish: FishInstance) = fish.x + fish.direction * w.mouthOffset

    fun press(s: FishingSession): FishingSession = when {
        s.paused || s.resumeSeconds > 0 -> s
        s.phase == FishingPhase.READY -> s.resetCast().copy(phase = FishingPhase.CHARGING, phaseSeconds = 0.0,
            power = 0.0, lastLoss = LossReason.NONE, lastObjectKind = null)
        s.phase in waterPhases -> s.copy(pulling = true)
        else -> s
    }
    fun release(s: FishingSession, cancel: Boolean = false): FishingSession = when {
        s.paused || s.resumeSeconds > 0 -> s.copy(pulling = false)
        s.phase == FishingPhase.CHARGING -> if (cancel) s.copy(phase = FishingPhase.READY,
            phaseSeconds = 0.0, pulling = false, power = 0.0) else cast(s)
        s.phase == FishingPhase.SEARCHING && s.pulling && !cancel -> reel(s).copy(pulling = false)
        else -> s.copy(pulling = false, hook = s.hook.copy(reelRemaining = if (s.phase == FishingPhase.SEARCHING) s.hook.reelRemaining else 0.0))
    }
    /** TalkBack: переключение удержания без необходимости физически держать палец. */
    fun toggle(s: FishingSession) = when (s.phase) {
        FishingPhase.CHARGING -> release(s)
        FishingPhase.SEARCHING -> reel(s)
        FishingPhase.FIGHTING, FishingPhase.HAULING -> if (s.pulling) release(s) else press(s)
        else -> press(s)
    }
    fun pause(s: FishingSession): FishingSession = release(s, cancel = true).copy(paused = true,
        pulling = false, resumeSeconds = 0.0)
    fun resume(s: FishingSession) = s.copy(paused = false, pulling = false, resumeSeconds = config.round.resumeCountdownSeconds)
    fun recast(s: FishingSession): FishingSession = if (s.phase == FishingPhase.SEARCHING && !s.paused && s.resumeSeconds <= 0)
        returning(s, LossReason.EMPTY, w.fastReturnSeconds) else s

    fun isBurst(s: FishingSession): Boolean {
        if (s.tutorial) return false
        val b = s.target?.let { config.fish(it.speciesId).burst } ?: return false
        return s.fightSeconds >= b.firstAtSeconds && (s.fightSeconds - b.firstAtSeconds) % b.periodSeconds < b.durationSeconds
    }
    fun burstSoon(s: FishingSession): Boolean {
        if (s.tutorial || s.phase != FishingPhase.FIGHTING) return false
        val b = s.target?.let { config.fish(it.speciesId).burst } ?: return false
        val until = if (s.fightSeconds < b.firstAtSeconds) b.firstAtSeconds - s.fightSeconds
            else b.periodSeconds - ((s.fightSeconds - b.firstAtSeconds) % b.periodSeconds)
        return until <= config.fight.burstWarningSeconds
    }
    /** Короткий импульс относится только к пустому крючку. */
    fun reel(s: FishingSession): FishingSession {
        if (s.paused || s.resumeSeconds > 0 || s.phase != FishingPhase.SEARCHING || s.activeSeconds - s.lastReelAt < w.reelInterval - EPS) return s
        return s.copy(hook = s.hook.copy(reelRemaining = s.hook.reelRemaining + w.emptyLiftImpulse, slowedDescent = true),
            lastReelAt = s.activeSeconds, reelCount = s.reelCount + 1)
    }
    fun requiredHoldSeconds(grams: Int): Double {
        val smallest = config.species.minOf { it.massGrams.min }
        val largest = config.species.maxOf { it.massGrams.max }
        val mass = ((grams - smallest).toDouble() / (largest - smallest)).coerceIn(0.0, 1.0)
        return w.smallFishHoldSeconds + (w.largeFishHoldSeconds - w.smallFishHoldSeconds) * mass
    }
    private fun FishingSession.resetCast() = copy(power = 0.0, tension = 0.0, warning = false,
        fightSeconds = 0.0, fightPullSeconds = 0.0, landingSpeed = 0.0, slackSeconds = 0.0,
        pulling = false, hook = hook.copy(reelRemaining = 0.0))
    private fun camera(s: FishingSession, dt: Double): FishingSession {
        val target = if (s.inWater || s.phase == FishingPhase.EMPTY_RETURN || s.phase == FishingPhase.CATCH_PENDING)
            (s.hook.y - w.cameraFollowDepth).coerceIn(0.0, w.maxDepth - .85) else 0.0
        val delta = (target - s.cameraDepth).coerceIn(-w.cameraSpeed * dt, w.cameraSpeed * dt)
        return s.copy(cameraDepth = s.cameraDepth + delta)
    }
    /** Остаток шага сохраняется: 30/60/120 FPS дают одинаковый мир и PRNG. */
    fun advance(initial: FishingSession, seconds: Double): FishingSession {
        require(seconds.isFinite() && seconds >= 0)
        if (initial.paused || initial.phase in stopped) return initial
        var s = initial
        var remaining = initial.simulationRemainder + seconds
        while (remaining + EPS >= w.fixedStep && s.phase !in stopped) {
            if (s.resumeSeconds > EPS) {
                val countdown = s.resumeSeconds - w.fixedStep
                s = s.copy(resumeSeconds = if (countdown <= EPS) 0.0 else countdown)
            } else s = step(if (s.resumeSeconds == 0.0) s else s.copy(resumeSeconds = 0.0), w.fixedStep)
            remaining -= w.fixedStep
        }
        return s.copy(simulationRemainder = if (s.phase in stopped) 0.0 else remaining.coerceAtLeast(0.0))
    }
    fun confirmCatch(s: FishingSession, now: Long): FishingSession {
        require(s.phase == FishingPhase.CATCH_PENDING)
        val fish = requireNotNull(s.target)
        val catch = FishingCatch(s.id + ":" + (s.catches.size + 1), fish.speciesId,
            if (s.tutorial) config.tutorial.forcedMassGrams else fish.grams, now)
        return retireTarget(s).resetCast().copy(phase = FishingPhase.RELEASING, phaseSeconds = 0.0,
            catches = s.catches + catch, pulling = false, bannerSeconds = config.round.catchSummaryVisibleSeconds)
            .event(FishingEventKind.CATCH, catch.speciesId, catch.grams.toDouble())
    }
    private fun cast(s: FishingSession): FishingSession {
        return s.copy(tension = 0.0, warning = false, fightSeconds = 0.0, fightPullSeconds = 0.0, landingSpeed = 0.0, slackSeconds = 0.0,
            phase = FishingPhase.FLYING, phaseSeconds = 0.0, castX = splashX(s),
            targetSpawnId = null, attachedObjectId = null, validCasts = s.validCasts + 1, pulling = false,
            hook = Hook(rodX(s), w.rodDepth, lineLength = w.maxLineLength)).event(FishingEventKind.CAST, "", s.power)
    }
    private fun step(before: FishingSession, dt: Double): FishingSession {
        if (before.phase == FishingPhase.BOMB_POP) {
            val elapsed = before.phaseSeconds + dt
            return if (elapsed >= w.bombPopSeconds) before.copy(phase = FishingPhase.FINISHED, phaseSeconds = elapsed)
                .event(FishingEventKind.FINISH, FinishReason.BOMB.name, before.totalGrams.toDouble())
            else before.copy(phaseSeconds = elapsed)
        }
        if (before.phase == FishingPhase.COUNTDOWN) {
            val elapsed = before.phaseSeconds + dt
            val phase = if (elapsed + EPS >= config.round.countdownSeconds) FishingPhase.READY else FishingPhase.COUNTDOWN
            return before.copy(phase = phase, phaseSeconds = if (phase == FishingPhase.READY) 0.0 else elapsed)
        }
        val world = moveWorld(before, dt)
        val s = angler(before.copy(fish = world.fish, objects = world.objects, rng = world.rng,
            nextSpawnId = world.nextSpawnId, bombSpawned = world.bombSpawned), before.fish, dt)
        return camera(s, dt)
    }
    private fun angler(before: FishingSession, previousFish: List<FishInstance>, dt: Double): FishingSession {
        if (before.phase == FishingPhase.FINISHED) return before
        var s = before.copy(phaseSeconds = before.phaseSeconds + dt, activeSeconds = before.activeSeconds + dt,
            bannerSeconds = (before.bannerSeconds - dt).coerceAtLeast(0.0))
        when (s.phase) {
            FishingPhase.CHARGING -> s = s.copy(power = (s.phaseSeconds / config.input.chargeSeconds).coerceIn(0.0, 1.0))
            FishingPhase.FLYING -> {
                val t = (s.phaseSeconds / flightDuration(s)).coerceIn(0.0, 1.0)
                s = s.copy(hook = s.hook.copy(x = rodX(s) + (s.castX - rodX(s)) * t,
                    y = w.rodDepth * (1 - t) - sin(t * PI) * .14))
                if (t >= 1) s = s.copy(phase = FishingPhase.SEARCHING, phaseSeconds = 0.0)
            }
            in waterPhases -> {
                s = advanceWater(s, previousFish, dt)
                if (s.phase == FishingPhase.CATCH_PENDING) return s
            }
            FishingPhase.RELEASING -> if (s.phaseSeconds + EPS >= config.round.catchReleaseSeconds)
                s = s.copy(phase = if (s.tutorial) FishingPhase.FINISHED else FishingPhase.READY, phaseSeconds = 0.0)
            FishingPhase.JUNK_RELEASE -> if (s.phaseSeconds >= w.junkReleaseSeconds) s = s.copy(phase = FishingPhase.READY, phaseSeconds = 0.0)
            FishingPhase.EMPTY_RETURN -> {
                val t = (s.phaseSeconds / s.returnDuration).coerceIn(0.0, 1.0)
                s = s.copy(hook = s.hook.copy(x = s.returnStart.x + (rodX(s) - s.returnStart.x) * t,
                    y = s.returnStart.y + (w.rodDepth - s.returnStart.y) * t))
                if (t >= 1) s = s.copy(phase = FishingPhase.READY, phaseSeconds = 0.0)
            }
            FishingPhase.BOMB_POP -> if (s.phaseSeconds >= w.bombPopSeconds) s = s.copy(phase = FishingPhase.FINISHED, pulling = false)
            else -> Unit
        }
        if (!s.tutorial && s.activeSeconds + EPS >= config.round.durationSeconds && s.finishReason != FinishReason.BOMB) {
            s = s.copy(phase = FishingPhase.FINISHED, activeSeconds = config.round.durationSeconds,
                finishReason = FinishReason.TIME, pulling = false)
        }
        if (s.phase != before.phase) s = when (s.phase) {
            FishingPhase.FIGHTING -> s.event(FishingEventKind.BITE, s.targetSpawnId.toString())
            FishingPhase.HAULING -> s.event(FishingEventKind.SNAG, s.attachedObject?.kind?.name.orEmpty())
            FishingPhase.JUNK_RELEASE, FishingPhase.BOMB_POP -> s.event(FishingEventKind.OBJECT_DELIVERED, s.lastObjectKind?.name.orEmpty())
            FishingPhase.EMPTY_RETURN -> if (s.lastLoss in setOf(LossReason.TENSION, LossReason.SLACK)) s.event(FishingEventKind.LOSS, s.lastLoss.name) else s
            FishingPhase.FINISHED -> s.event(FishingEventKind.FINISH, s.finishReason.name, s.totalGrams.toDouble())
            else -> s
        }
        return s
    }
    private fun FishingSession.event(kind: FishingEventKind, detail: String, value: Double = 0.0) =
        copy(recentEvents = (recentEvents + FishingEvent(kind, activeSeconds, detail, value)).takeLast(32))

    private fun advanceWater(initial: FishingSession, previousFish: List<FishInstance>, dt: Double): FishingSession {
        // Предел натяжения проверяем до доставки: 100% нельзя спасти отпусканием или близостью лодки.
        var s = if (initial.phase == FishingPhase.FIGHTING) fight(initial, dt) else initial
        if (s.phase !in waterPhases) return s
        val oldHook = s.hook
        s = moveHook(s, dt)
        val contact = intersection(oldHook.x - rodX(s), oldHook.y - w.landingDepth,
            s.hook.x - rodX(s), s.hook.y - w.landingDepth, w.landingRadius)
        val arrivedAt = s.activeSeconds - dt + dt * (contact ?: 1.0)
        val beforeDeadline = s.tutorial || arrivedAt < config.round.durationSeconds - EPS
        val delivering = if (s.phase == FishingPhase.SEARCHING) s.reelCount > 0 && s.activeSeconds - s.lastReelAt < 1 else s.pulling
        if (contact != null && beforeDeadline && delivering) {
            s = when (s.phase) {
                FishingPhase.FIGHTING -> if (s.fightPullSeconds + EPS >= requiredHoldSeconds(requireNotNull(s.target).grams) && (!s.tutorial || s.tutorialReleased))
                    s.copy(phase = FishingPhase.CATCH_PENDING, pulling = false, activeSeconds = arrivedAt) else s
                FishingPhase.HAULING -> deliverObject(s)
                else -> returning(s, LossReason.EMPTY, w.lostReturnSeconds)
            }
        }
        return if (s.phase == FishingPhase.SEARCHING) encounter(s, oldHook, previousFish) else s
    }
    private fun moveHook(s: FishingSession, dt: Double): FishingSession {
        val h = s.hook
        val emptyHook = s.phase == FishingPhase.SEARCHING
        val dx = if (emptyHook) 0.0 else rodX(s) - h.x
        val dy = if (emptyHook) -1.0 else w.landingDepth - h.y
        val distance = hypot(dx, dy).coerceAtLeast(.001)
        val pendingEffort = s.target?.let { s.fightPullSeconds + EPS < requiredHoldSeconds(it.grams) } == true
        val allowance = if (emptyHook) h.y.coerceAtLeast(0.0)
            else if (pendingEffort) (distance - w.landingRadius * 1.15).coerceAtLeast(0.0) else distance
        val speed = if (s.target == null) w.objectReelSpeed else s.landingSpeed.takeIf { it > 0 }
            ?: distance / requiredHoldSeconds(requireNotNull(s.target).grams)
        val requested = if (emptyHook) min(h.reelRemaining, w.impulseSpeed * dt)
            else if (s.pulling) speed * dt * (if (isBurst(s)) config.fight.burstReelMultiplier else 1.0) else 0.0
        val reeling = min(requested, allowance)
        var x = h.x + if (reeling > 0) dx / distance * reeling else w.currentSpeed * dt
        var y = h.y + if (reeling > 0) dy / distance * reeling else w.sinkSpeed * dt * (if (s.phase == FishingPhase.FIGHTING) .15
            else if (emptyHook && h.slowedDescent) w.postReelSinkMultiplier else 1.0)
        val extent = hypot(x - rodX(s), y - w.rodDepth)
        // Пустой крючок после импульса свободно разматывает леску и не зависает.
        val line = if (emptyHook) w.maxLineLength else if (reeling > 0) extent.coerceAtLeast(.1) else h.lineLength
        if (extent > line) {
            x = rodX(s) + (x - rodX(s)) / extent * line
            y = w.rodDepth + (y - w.rodDepth) / extent * line
        }
        if (s.tutorial && !s.tutorialReleased && y < .14) y = .14
        x = x.coerceIn(.04, .96)
        y = y.coerceIn(w.rodDepth, w.maxDepth)
        val hook = h.copy(x = x, y = y,
            vx = (x - h.x) / dt, vy = (y - h.y) / dt, lineLength = line, reelRemaining = if (emptyHook) (h.reelRemaining - requested).coerceAtLeast(0.0) else 0.0)
        return s.copy(hook = hook, reelingSeconds = s.reelingSeconds + if (!emptyHook && s.pulling) dt else 0.0,
            lastReelAt = if (!emptyHook && s.pulling) s.activeSeconds else s.lastReelAt, fish = s.fish.map { if (it.spawnId == s.targetSpawnId)
            it.copy(x = hook.x + w.mouthOffset, y = hook.y,
                direction = -1, behavior = FishBehavior.HOOKED) else it },
            objects = s.objects.map { if (it.id == s.attachedObjectId) it.copy(x = hook.x, y = hook.y) else it })
    }
    private data class Contact(val time: Double, val id: Int, val fish: Boolean)
    private fun encounter(s: FishingSession, oldHook: Hook, previousFish: List<FishInstance>): FishingSession {
        val contacts = mutableListOf<Contact>()
        for (fish in s.fish) {
            if (fish.respawnSeconds > 0 || fish.arrivalSeconds > 0 || fish.behavior == FishBehavior.HOOKED) continue
            val previous = previousFish.firstOrNull { it.spawnId == fish.spawnId } ?: fish
            val offset = fish.direction * w.catchBodyOffset
            intersection((oldHook.x - previous.x - offset) / w.catchRadiusX, (oldHook.y - previous.y) / w.catchRadiusY,
                (s.hook.x - fish.x - offset) / w.catchRadiusX, (s.hook.y - fish.y) / w.catchRadiusY, 1.0)?.let { contacts += Contact(it, fish.spawnId, true) }
        }
        for (obj in s.objects) {
            if (obj.taken || obj.kind == ObjectKind.BOMB && obj.visibleSeconds < w.bombVisibleSeconds) continue
            intersection(oldHook.x - obj.x, oldHook.y - obj.y, s.hook.x - obj.x, s.hook.y - obj.y,
                w.objectRadius)?.let { contacts += Contact(it, obj.id, false) }
        }
        val first = contacts.minWithOrNull(compareBy<Contact> { it.time }.thenBy { it.id }) ?: return s
        return if (first.fish) s.copy(phase = FishingPhase.FIGHTING, phaseSeconds = 0.0, targetSpawnId = first.id,
            fish = s.fish.map { if (it.spawnId == first.id) it.copy(behavior = FishBehavior.HOOKED) else it },
            fightSeconds = 0.0, fightPullSeconds = 0.0,
            landingSpeed = hypot(s.hook.x - rodX(s), s.hook.y - w.landingDepth) /
                requiredHoldSeconds(s.fish.first { it.spawnId == first.id }.grams),
            tension = config.fight.initialTension, warning = false,
            slackSeconds = 0.0, lastReelAt = min(s.lastReelAt, s.activeSeconds - w.reelInterval), tutorialReleased = false,
            tutorialWarningSeen = false, hook = s.hook.copy(reelRemaining = 0.0))
        else s.copy(phase = FishingPhase.HAULING, phaseSeconds = 0.0, attachedObjectId = first.id,
            objects = s.objects.map { if (it.id == first.id) it.copy(taken = true) else it },
            hook = s.hook.copy(reelRemaining = 0.0), tension = 0.0, warning = false)
    }
    private fun fight(s: FishingSession, dt: Double): FishingSession {
        if (!s.tutorial && s.tension >= config.fight.maxTension - EPS) return lost(s, LossReason.TENSION)
        val rule = config.fish(requireNotNull(s.target).speciesId)
        val rate = if (s.pulling) rule.tensionPerSecond + (if (isBurst(s)) rule.burst!!.tensionPerSecond else 0.0)
            else -config.fight.releaseCoolingPerSecond
        val tension = (s.tension + rate * dt).coerceIn(0.0, if (s.tutorial) config.tutorial.maxTension else config.fight.maxTension)
        val slack = if (s.pulling) 0.0 else s.slackSeconds + dt
        val next = s.copy(tension = tension, fightSeconds = s.fightSeconds + dt,
            fightPullSeconds = s.fightPullSeconds + if (s.pulling) dt else 0.0,
            slackSeconds = slack, warning = tension >= config.fight.warningOn || s.warning && tension > config.fight.warningOff,
            tutorialWarningSeen = s.tutorialWarningSeen || tension >= config.fight.warningOn,
            tutorialReleased = s.tutorialReleased || s.tutorialWarningSeen && slack >= config.tutorial.minimumReleaseSeconds)
        return when {
            !s.tutorial && tension >= config.fight.maxTension - EPS -> lost(next, LossReason.TENSION)
            !s.tutorial && slack + EPS >= config.fight.escapeAfterContinuousReleaseSeconds -> lost(next, LossReason.SLACK)
            else -> next
        }
    }
    private fun deliverObject(s: FishingSession): FishingSession {
        val obj = requireNotNull(s.attachedObject)
        return s.resetCast().copy(phase = if (obj.kind == ObjectKind.BOMB) FishingPhase.BOMB_POP else FishingPhase.JUNK_RELEASE,
            phaseSeconds = 0.0, attachedObjectId = null, pulling = false, lastObjectKind = obj.kind,
            junkCount = s.junkCount + if (obj.kind == ObjectKind.BOMB) 0 else 1,
            finishReason = if (obj.kind == ObjectKind.BOMB) FinishReason.BOMB else s.finishReason)
    }
    private fun returning(s: FishingSession, reason: LossReason, seconds: Double) = s.resetCast().copy(
        phase = FishingPhase.EMPTY_RETURN, phaseSeconds = 0.0, lastLoss = reason, pulling = false,
        returnStart = s.hook, returnDuration = seconds, targetSpawnId = null, attachedObjectId = null,
        hook = s.hook.copy(reelRemaining = 0.0), warning = false)
    private fun lost(s: FishingSession, reason: LossReason) = returning(retireTarget(s), reason, w.lostReturnSeconds)
        .copy(lossCount = s.lossCount + 1, lostSpawnId = s.targetSpawnId)
    private fun retireTarget(s: FishingSession) = s.copy(fish = s.fish.map { if (it.spawnId == s.targetSpawnId)
        it.copy(respawnSeconds = config.population.respawnDelaySeconds, behavior = FishBehavior.SWIMMING) else it }, targetSpawnId = null)

    private fun moveWorld(s: FishingSession, dt: Double): FishingSession {
        var rng = s.rng
        var nextId = s.nextSpawnId
        val fish = s.fish.map { source ->
            val f = source.copy(arrivalSeconds = (source.arrivalSeconds - dt).coerceAtLeast(0.0))
            val rule = config.fish(f.speciesId)
            when {
                f.respawnSeconds > dt -> f.copy(respawnSeconds = f.respawnSeconds - dt)
                f.respawnSeconds > 0 -> {
                    rng = nextRandom(rng)
                    val count = (rule.massGrams.max - rule.massGrams.min) / config.population.massStepGrams + 1
                    val x = if (rng % 2L == 0L) rule.worldXPatrol[0] else rule.worldXPatrol[1]
                    f.copy(spawnId = nextId++, grams = rule.massGrams.min + (rng % count).toInt() * config.population.massStepGrams,
                        x = x, y = (rule.waterDepth[0] + rule.waterDepth[1]) / 2, respawnSeconds = 0.0, arrivalSeconds = .45,
                        direction = if (x == rule.worldXPatrol[0]) 1 else -1, vx = w.swimSpeed,
                        swimRng = nextRandom(rng), turnSeconds = w.turnMinSeconds)
                }
                f.behavior == FishBehavior.HOOKED -> f
                else -> {
                    // Маршрут зависит только от рыбы и её PRNG, никогда от положения крючка.
                    var swimRng = f.swimRng
                    var turnSeconds = f.turnSeconds - dt
                    var direction = f.direction
                    var vy = f.vy
                    if (turnSeconds <= 0) {
                        swimRng = nextRandom(swimRng)
                        if (randomUnit(swimRng) < .55) direction = -direction
                        swimRng = nextRandom(swimRng)
                        vy = (randomUnit(swimRng) * 2 - 1) * w.verticalSwimSpeed
                        swimRng = nextRandom(swimRng)
                        turnSeconds = w.turnMinSeconds + randomUnit(swimRng) * (w.turnMaxSeconds - w.turnMinSeconds)
                    }
                    val left = rule.worldXPatrol[0]; val right = rule.worldXPatrol[1]
                    val top = rule.waterDepth[0]; val bottom = rule.waterDepth[1]
                    if (f.x <= left && direction < 0 || f.x >= right && direction > 0) direction = -direction
                    if (f.y <= top && vy < 0 || f.y >= bottom && vy > 0) vy = -vy
                    val speed = w.swimSpeed * (.85 + (f.spawnId % 4) * .1)
                    f.copy(x = (f.x + direction * speed * dt).coerceIn(left, right),
                        y = (f.y + vy * dt).coerceIn(top, bottom), vx = direction * speed, vy = vy,
                        direction = direction, behavior = FishBehavior.SWIMMING,
                        swimRng = swimRng, turnSeconds = turnSeconds)
                }
            }
        }
        var objects = s.objects.map { it.copy(visibleSeconds = it.visibleSeconds + dt) }
        var spawned = s.bombSpawned
        if (!s.tutorial && !spawned && s.activeSeconds >= w.bombAfterSeconds) {
            val position = listOf(.69 to .84, .87 to .77, .54 to .88).firstOrNull { (x, y) ->
                hypot(s.hook.x - x, s.hook.y - y) > .18 &&
                    intersection(x - s.hook.x, y - s.hook.y, x - rodX(s), y - w.landingDepth, .09) == null &&
                    fish.all { hypot(it.x - x, it.y - y) > .09 }
            }
            if (position != null) { objects = objects + PondObject(10003, ObjectKind.BOMB, position.first, position.second); spawned = true }
        }
        return s.copy(fish = fish, objects = objects, bombSpawned = spawned, rng = rng, nextSpawnId = nextId)
    }
    /** Время первого контакта движущихся точек внутри шага. */
    private fun intersection(x0: Double, y0: Double, x1: Double, y1: Double, radius: Double): Double? {
        val c = x0 * x0 + y0 * y0 - radius * radius
        if (c <= 0) return 0.0
        val dx = x1 - x0; val dy = y1 - y0
        val a = dx * dx + dy * dy
        if (a < EPS * EPS) return null
        val b = 2 * (x0 * dx + y0 * dy)
        val discriminant = b * b - 4 * a * c
        if (discriminant < 0) return null
        return ((-b - sqrt(discriminant)) / (2 * a)).takeIf { it >= -EPS && it <= 1 + EPS }?.coerceIn(0.0, 1.0)
    }
    private fun nextRandom(rng: Long) = (rng * 1103515245L + 12345L) and 0x7fffffffL
    private fun randomUnit(rng: Long) = rng.toDouble() / 0x7fffffffL
    private companion object { const val EPS = 1e-9 }
}
