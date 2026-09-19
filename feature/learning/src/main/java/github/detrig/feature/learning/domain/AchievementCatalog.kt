package github.detrig.feature.learning.domain

object LearningTopicIds {
    const val BUDGET_PLANNING = "budget_planning"
    const val SAVINGS_BUILDING = "savings_building"
    const val PAYMENTS_AND_PURCHASES = "payments_and_purchases"
    const val FINANCIAL_SECURITY = "financial_security"
}

object LearningMetricIds {
    const val BUDGET_REASONABLE_PLAN = "budget.reasonable_plan"
    const val BUDGET_FOLLOW_PLAN = "budget.follow_plan"
    const val BUDGET_ADAPT_TO_CHANGE = "budget.adapt_to_change"

    const val SAVINGS_CREATE_GOAL = "savings.create_goal"
    const val SAVINGS_PLAN = "savings.plan_saving"
    const val SAVINGS_REGULAR_CONTRIBUTION = "savings.regular_contribution"
    const val SAVINGS_REACH_GOAL = "savings.reach_goal"

    const val PURCHASE_REASONABLE = "purchase.reasonable_purchase"
    const val PURCHASE_PROMOTION = "purchase.promotion_decision"
    const val PURCHASE_IMPULSE = "purchase.impulse_decision"

    const val SECURITY_CONFIRMATION_CODE = "security.confirmation_code_request"
    const val SECURITY_UNKNOWN_LINK = "security.unknown_link"
}

class AchievementCatalog(
    definitions: List<AchievementDefinition>,
) {
    val definitions: List<AchievementDefinition> = definitions.sortedBy { it.order }

    private val byId = this.definitions.associateBy { it.achievementId }
    private val byMetric = this.definitions.groupBy { it.metricId }

    init {
        require(this.definitions.isNotEmpty())
        require(byId.size == this.definitions.size) { "Achievement IDs must be unique" }
        require(this.definitions.map { it.order }.distinct().size == this.definitions.size) {
            "Achievement order values must be unique"
        }
        byMetric.forEach { (metricId, metricDefinitions) ->
            require(metricDefinitions.size == 2) { "$metricId must have exactly two achievements" }
            require(metricDefinitions.map { it.stage }.toSet() == AchievementStage.entries.toSet()) {
                "$metricId must have Introduction and Learned achievements"
            }
        }
    }

    fun definition(achievementId: String): AchievementDefinition? = byId[achievementId]

    fun definitionsForMetric(metricId: String): List<AchievementDefinition> =
        byMetric[metricId].orEmpty()

    fun containsMetric(metricId: String): Boolean = byMetric.containsKey(metricId)
}

object MvpAchievementCatalog {
    fun create(config: LearningConfig): AchievementCatalog {
        var order = 0
        fun achievement(
            metricId: String,
            topicId: String,
            stage: AchievementStage,
            childTitle: String,
            childDescription: String,
            parentText: String,
            learnedThreshold: Int = 2,
        ): AchievementDefinition {
            val suffix = if (stage == AchievementStage.INTRODUCTION) "introduction" else "learned"
            val id = "$metricId.$suffix"
            return AchievementDefinition(
                achievementId = id,
                topicId = topicId,
                metricId = metricId,
                stage = stage,
                requiredProgressSteps = if (stage == AchievementStage.INTRODUCTION) 1 else learnedThreshold,
                xpReward = config.xpFor(id),
                childTitle = childTitle,
                childDescription = childDescription,
                parentText = parentText,
                order = order++,
            )
        }

        val definitions = buildList {
            add(achievement(
                LearningMetricIds.BUDGET_REASONABLE_PLAN,
                LearningTopicIds.BUDGET_PLANNING,
                AchievementStage.INTRODUCTION,
                "Первый план",
                "Ты познакомился с распределением денег на неделю.",
                "Ребёнок ознакомился с распределением денег между обязательными расходами, желаниями, сбережениями и финансовой подушкой.",
            ))
            add(achievement(
                LearningMetricIds.BUDGET_REASONABLE_PLAN,
                LearningTopicIds.BUDGET_PLANNING,
                AchievementStage.LEARNED,
                "Продуманный план",
                "В твоём плане хватает денег на обязательные расходы.",
                "Ребёнок научился составлять недельный план, в котором хватает денег на обязательные расходы.",
            ))
            add(achievement(
                LearningMetricIds.BUDGET_FOLLOW_PLAN,
                LearningTopicIds.BUDGET_PLANNING,
                AchievementStage.INTRODUCTION,
                "План и результат",
                "Ты сравнил план с настоящими расходами.",
                "Ребёнок ознакомился со сравнением недельного плана с фактическими расходами.",
            ))
            add(achievement(
                LearningMetricIds.BUDGET_FOLLOW_PLAN,
                LearningTopicIds.BUDGET_PLANNING,
                AchievementStage.LEARNED,
                "Следую плану",
                "Ты научился учитывать свой план несколько игровых периодов.",
                "Ребёнок научился следовать недельному плану на протяжении нескольких игровых периодов.",
            ))
            add(achievement(
                LearningMetricIds.BUDGET_ADAPT_TO_CHANGE,
                LearningTopicIds.BUDGET_PLANNING,
                AchievementStage.INTRODUCTION,
                "Бюджет меняется",
                "Ты увидел, как неожиданность влияет на оставшиеся деньги.",
                "Ребёнок ознакомился с тем, как неожиданный расход, дополнительный доход или изменение цены влияет на бюджет.",
            ))
            add(achievement(
                LearningMetricIds.BUDGET_ADAPT_TO_CHANGE,
                LearningTopicIds.BUDGET_PLANNING,
                AchievementStage.LEARNED,
                "Гибкий бюджет",
                "Ты научился менять решения, когда финансовая ситуация изменилась.",
                "Ребёнок научился адаптировать оставшиеся расходы к изменившейся финансовой ситуации.",
            ))

            add(achievement(
                LearningMetricIds.SAVINGS_CREATE_GOAL,
                LearningTopicIds.SAVINGS_BUILDING,
                AchievementStage.INTRODUCTION,
                "Моя цель",
                "Ты познакомился с финансовой целью.",
                "Ребёнок ознакомился с созданием финансовой цели.",
            ))
            add(achievement(
                LearningMetricIds.SAVINGS_CREATE_GOAL,
                LearningTopicIds.SAVINGS_BUILDING,
                AchievementStage.LEARNED,
                "Сам выбираю цель",
                "Ты научился выбирать достижимую финансовую цель.",
                "Ребёнок научился самостоятельно выбирать и создавать достижимую финансовую цель.",
            ))
            add(achievement(
                LearningMetricIds.SAVINGS_PLAN,
                LearningTopicIds.SAVINGS_BUILDING,
                AchievementStage.INTRODUCTION,
                "Коплю по плану",
                "Ты добавил сбережения в недельный план.",
                "Ребёнок ознакомился с включением сбережений в недельный план.",
            ))
            add(achievement(
                LearningMetricIds.SAVINGS_PLAN,
                LearningTopicIds.SAVINGS_BUILDING,
                AchievementStage.LEARNED,
                "Регулярный план",
                "Ты научился заранее планировать пополнения цели.",
                "Ребёнок научился планировать регулярные пополнения финансовой цели.",
            ))
            add(achievement(
                LearningMetricIds.SAVINGS_REGULAR_CONTRIBUTION,
                LearningTopicIds.SAVINGS_BUILDING,
                AchievementStage.INTRODUCTION,
                "Первое пополнение",
                "Ты впервые отложил деньги на цель.",
                "Ребёнок ознакомился с откладыванием денег на финансовую цель.",
            ))
            add(achievement(
                LearningMetricIds.SAVINGS_REGULAR_CONTRIBUTION,
                LearningTopicIds.SAVINGS_BUILDING,
                AchievementStage.LEARNED,
                "Коплю регулярно",
                "Ты пополнял цель в нескольких игровых периодах.",
                "Ребёнок научился пополнять финансовую цель на протяжении нескольких игровых периодов.",
            ))
            add(achievement(
                LearningMetricIds.SAVINGS_REACH_GOAL,
                LearningTopicIds.SAVINGS_BUILDING,
                AchievementStage.INTRODUCTION,
                "Цель собрана",
                "Ты накопил нужную сумму на выбранную цель.",
                "Ребёнок ознакомился с завершением накопления на выбранную цель.",
            ))
            add(achievement(
                LearningMetricIds.SAVINGS_REACH_GOAL,
                LearningTopicIds.SAVINGS_BUILDING,
                AchievementStage.LEARNED,
                "Довожу до цели",
                "Ты научился завершать план накопления.",
                "Ребёнок научился доводить план накопления до достижения цели.",
            ))

            add(achievement(
                LearningMetricIds.PURCHASE_REASONABLE,
                LearningTopicIds.PAYMENTS_AND_PURCHASES,
                AchievementStage.INTRODUCTION,
                "Проверяю покупку",
                "Ты проверил покупку по бюджету и обязательным нуждам.",
                "Ребёнок ознакомился с проверкой того, подходит ли покупка текущему бюджету и обязательным потребностям.",
            ))
            add(achievement(
                LearningMetricIds.PURCHASE_REASONABLE,
                LearningTopicIds.PAYMENTS_AND_PURCHASES,
                AchievementStage.LEARNED,
                "Разумная покупка",
                "Ты научился учитывать деньги и будущие обязательные расходы.",
                "Ребёнок научился оценивать покупку с учётом доступных денег и предстоящих обязательных расходов.",
            ))
            add(achievement(
                LearningMetricIds.PURCHASE_PROMOTION,
                LearningTopicIds.PAYMENTS_AND_PURCHASES,
                AchievementStage.INTRODUCTION,
                "Проверяю акцию",
                "Ты познакомился с проверкой пользы акции.",
                "Ребёнок ознакомился с проверкой реальной пользы акции.",
            ))
            add(achievement(
                LearningMetricIds.PURCHASE_PROMOTION,
                LearningTopicIds.PAYMENTS_AND_PURCHASES,
                AchievementStage.LEARNED,
                "Полезная скидка",
                "Ты научился оценивать акцию по цене и необходимости.",
                "Ребёнок научился оценивать акции с учётом цены, необходимости покупки и доступных денег.",
            ))
            add(achievement(
                LearningMetricIds.PURCHASE_IMPULSE,
                LearningTopicIds.PAYMENTS_AND_PURCHASES,
                AchievementStage.INTRODUCTION,
                "Сначала подумаю",
                "Ты сделал паузу перед импульсивной покупкой.",
                "Ребёнок ознакомился с паузой перед импульсивной покупкой.",
            ))
            add(achievement(
                LearningMetricIds.PURCHASE_IMPULSE,
                LearningTopicIds.PAYMENTS_AND_PURCHASES,
                AchievementStage.LEARNED,
                "Осознанное желание",
                "Ты научился проверять желание по бюджету и обязательным расходам.",
                "Ребёнок научился оценивать импульсивное желание, не ставя под угрозу обязательные расходы.",
            ))

            add(achievement(
                LearningMetricIds.SECURITY_CONFIRMATION_CODE,
                LearningTopicIds.FINANCIAL_SECURITY,
                AchievementStage.INTRODUCTION,
                "Подозрительный код",
                "Ты узнал, почему нельзя сообщать код подтверждения.",
                "Ребёнок ознакомился с тем, почему просьба сообщить код подтверждения подозрительна.",
                learnedThreshold = 3,
            ))
            add(achievement(
                LearningMetricIds.SECURITY_CONFIRMATION_CODE,
                LearningTopicIds.FINANCIAL_SECURITY,
                AchievementStage.LEARNED,
                "Код держу в секрете",
                "Ты умеешь защитить код и обратиться к взрослому.",
                "Ребёнок научился не сообщать коды подтверждения и обращаться за помощью к взрослому.",
                learnedThreshold = 3,
            ))
            add(achievement(
                LearningMetricIds.SECURITY_UNKNOWN_LINK,
                LearningTopicIds.FINANCIAL_SECURITY,
                AchievementStage.INTRODUCTION,
                "Неизвестная ссылка",
                "Ты узнал, почему незнакомая ссылка может быть опасна.",
                "Ребёнок ознакомился с тем, почему неизвестная ссылка может быть опасна.",
                learnedThreshold = 3,
            ))
            add(achievement(
                LearningMetricIds.SECURITY_UNKNOWN_LINK,
                LearningTopicIds.FINANCIAL_SECURITY,
                AchievementStage.LEARNED,
                "Безопасный выбор",
                "Ты умеешь не открывать неизвестные ссылки.",
                "Ребёнок научился не переходить по неизвестным ссылкам и самостоятельно выбирать безопасную реакцию.",
                learnedThreshold = 3,
            ))
        }

        val catalog = AchievementCatalog(definitions)
        val unknownOverrides = config.achievementXpOverrides.keys - definitions.map { it.achievementId }.toSet()
        require(unknownOverrides.isEmpty()) { "XP overrides reference unknown achievements: $unknownOverrides" }
        config.metricRules.forEach { rule ->
            require(catalog.containsMetric(rule.metricId)) { "Rule references unknown metric ${rule.metricId}" }
            val maximumStep = rule.milestones.maxOf { it.progressSteps }
            val requiredStep = catalog.definitionsForMetric(rule.metricId).maxOf { it.requiredProgressSteps }
            require(maximumStep >= requiredStep) {
                "Rule for ${rule.metricId} cannot unlock all achievements"
            }
        }
        return catalog
    }
}
