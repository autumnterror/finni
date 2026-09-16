package github.detrig.minigames.flight.domain

import kotlin.math.max
import kotlin.math.min

/** Детерминированная физика; время и ввод не зависят от Android и частоты экрана. */
class FlightEngine(val config: FlightConfig) {
    init { config.validate() }

    fun create(id: String, profileId: String, appearanceId: String, seed: Int,
        startedAtMillis: Long, training: Boolean = false): FlightSession {
        require(id.isNotBlank() && profileId.isNotBlank())
        var session = FlightSession(id, profileId, config.rulesVersion, appearanceId, seed,
            seed.takeUnless { it == 0 } ?: 1, startedAtMillis, training,
            y = config.world.startY, lastCenter = config.world.startY)
        session = appendGate(session, config.gates.firstX)
        return appendGate(session, config.gates.firstX + config.gates.spacing)
    }

    /** Автовзлёт после отсчёта не считается действием игрока. */
    fun launch(session: FlightSession): FlightSession =
        if (session.started || session.outcome != null) session
        else session.copy(started = true, velocity = config.physics.flapVelocity)

    fun flap(session: FlightSession): FlightSession {
        if (session.outcome != null || session.tick - session.lastFlapTick < config.physics.minFlapTicks) return session
        return session.copy(started = true, velocity = config.physics.flapVelocity,
            flapCount = session.flapCount + 1, lastFlapTick = session.tick)
    }

    fun step(session: FlightSession): FlightSession = advance(session, 1)

    fun advance(session: FlightSession, ticks: Int): FlightSession {
        require(ticks >= 0)
        if (ticks == 0 || !session.started || session.outcome != null) return session
        val dt = 1.0 / config.physics.tickRate
        val world = config.world
        var tick = session.tick
        var y = session.y
        var velocity = session.velocity
        var score = session.score
        var rng = session.rng
        var nextGateId = session.nextGateId
        var lastCenter = session.lastCenter
        var outcome: FlightOutcome? = null
        val gates = ArrayList(session.gates)
        // Один снимок на кадр вместо нескольких снимков и списков на каждый шаг.
        var steps = 0
        while (steps++ < ticks && outcome == null) {
            val stage = stage(tick, session.training)
            velocity = min(velocity + config.physics.gravity * dt, config.physics.maxFallVelocity)
            y += velocity * dt
            tick++
            var hit = -1
            for (i in gates.indices) {
                val moved = gates[i].copy(x = gates[i].x - stage.speed * dt)
                gates[i] = moved
                if (hit < 0 && collides(y, moved)) hit = i
            }
            val boundary = y - world.radius <= world.top || y + world.radius >= world.bottom
            // Столкновение раньше очка и финиша на том же шаге.
            if (hit >= 0 || boundary) {
                if (!session.training) { outcome = FlightOutcome.LANDED; break }
                y = if (hit >= 0) gates[hit].center else world.startY
                velocity = 0.0
                if (hit >= 0) gates[hit] = gates[hit].copy(missed = true)
            }
            for (i in gates.indices) {
                val gate = gates[i]
                if (!gate.passed && gate.x + config.gates.width < world.petX - world.radius) {
                    if (!gate.missed) score++
                    gates[i] = gate.copy(passed = true)
                }
            }
            while (gates.isNotEmpty() && gates.first().x + config.gates.width <= 0) gates.removeAt(0)
            val lastX = gates.lastOrNull()?.x
            if (lastX == null || lastX < world.width + config.gates.spacing) {
                rng = nextRandom(rng)
                val gap = gap(nextGateId, tick, session.training)
                lastCenter = gateCenter(rng, nextGateId, lastCenter, gap, session.training)
                gates.add(FlightGate(nextGateId++, (lastX ?: world.width) + config.gates.spacing, lastCenter, gap))
            }
            if (session.training && score >= config.tutorial.targetGates ||
                !session.training && tick >= config.round.durationTicks) outcome = FlightOutcome.FINISHED
        }
        return session.copy(tick = tick, y = y, velocity = velocity, score = score, gates = gates,
            rng = rng, nextGateId = nextGateId, lastCenter = lastCenter, outcome = outcome)
    }

    fun collides(y: Double, gate: FlightGate): Boolean {
        val w = config.world
        return circleHitsRect(w.petX, y, w.radius, gate.x, w.top,
            gate.x + config.gates.width, gate.center - gate.gap / 2) ||
            circleHitsRect(w.petX, y, w.radius, gate.x, gate.center + gate.gap / 2,
                gate.x + config.gates.width, w.bottom)
    }

    private fun stage(tick: Int, training: Boolean) =
        if (training) config.stages.first() else config.stages.last { tick >= it.fromTick }

    private fun gap(id: Int, tick: Int, training: Boolean) =
        if (id < config.gates.firstEasyCount || training) config.gates.firstGap else stage(tick, training).gap

    private fun appendGate(session: FlightSession, x: Double): FlightSession {
        val rng = nextRandom(session.rng)
        val gap = gap(session.nextGateId, session.tick, session.training)
        val center = gateCenter(rng, session.nextGateId, session.lastCenter, gap, session.training)
        return session.copy(rng = rng, nextGateId = session.nextGateId + 1, lastCenter = center,
            gates = session.gates + FlightGate(session.nextGateId, x, center, gap))
    }

    private fun nextRandom(value: Int): Int {
        var rng = value
        rng = rng xor (rng shl 13)
        rng = rng xor (rng ushr 17)
        rng = rng xor (rng shl 5)
        return rng
    }

    private fun gateCenter(rng: Int, id: Int, last: Double, gap: Double, training: Boolean): Double {
        val unit = (rng.toLong() and 0xffffffffL).toDouble() / 4294967295.0
        return if (id < config.gates.firstEasyCount || training) config.world.startY else {
            val margin = config.gates.edgeMargin + gap / 2
            (last + (unit * 2 - 1) * config.gates.maxCenterShift)
                .coerceIn(margin, config.world.height - margin)
        }
    }

    companion object {
        fun circleHitsRect(cx: Double, cy: Double, radius: Double,
            left: Double, top: Double, right: Double, bottom: Double): Boolean {
            val dx = cx - max(left, min(cx, right))
            val dy = cy - max(top, min(cy, bottom))
            return dx * dx + dy * dy <= radius * radius
        }
    }
}