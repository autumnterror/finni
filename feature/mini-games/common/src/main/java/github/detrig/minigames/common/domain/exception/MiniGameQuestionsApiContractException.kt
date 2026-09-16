package github.detrig.minigames.common.domain.exception

import github.detrig.core.exception.AppException
import github.detrig.core.exception.ExceptionType

internal class MiniGameQuestionsApiContractException : AppException(ExceptionType.Application) {

    override val moduleCode: String
        get() = MODULE_CODE

    override val localCode: String
        get() = LOCAL_CODE

    private companion object {
        const val MODULE_CODE = "MINI_GAMES_COMMON"
        const val LOCAL_CODE = "QUESTIONS_API_CONTRACT"
    }
}