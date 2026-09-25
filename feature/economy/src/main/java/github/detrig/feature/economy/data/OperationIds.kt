package github.detrig.feature.economy.data

internal suspend fun firstAvailableOperationId(
    baseId: String,
    operationExists: suspend (String) -> Boolean,
): String {
    require(baseId.isNotBlank())
    var candidate = baseId
    var attempt = 0
    while (operationExists(candidate)) {
        attempt = Math.incrementExact(attempt)
        candidate = "$baseId:repeat:$attempt"
    }
    return candidate
}
