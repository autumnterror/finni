package github.detrig.core.exception

sealed class ExceptionType(val shortCode: String) {
    data object Network : ExceptionType("NET")
    data object AndroidInternal : ExceptionType("AI")
    data object Database : ExceptionType("DB")
    data object Application : ExceptionType("APP")
    data object BusinessLogic : ExceptionType("BL")
}
