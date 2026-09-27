package github.detrig.feature.learningtests.domain

internal object LearningTestCatalog {
    val tests = listOf(
        LearningTestDefinition(
            id = "money-flow",
            title = "Куда уходят деньги?",
            difficulty = LearningTestDifficulty.SIMPLE,
            questions = listOf(
                LearningTestQuestion(
                    prompt = "Что такое расход?",
                    options = listOf(
                        "Деньги, которые ты получил",
                        "Деньги, которые ты потратил",
                        "Деньги в копилке",
                        "Любая монета",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Ты получил 200 ₽ и купил сок за 60 ₽. Сколько осталось?",
                    options = listOf(
                        "120 ₽",
                        "140 ₽",
                        "160 ₽",
                        "260 ₽",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Что из этого является доходом?",
                    options = listOf(
                        "Покупка мороженого",
                        "Оплата игры",
                        "Полученные карманные деньги",
                        "Покупка игрушки",
                    ),
                    correctOptionIndex = 2,
                ),
                LearningTestQuestion(
                    prompt = "У тебя 300 ₽. Можно ли купить игрушку за 350 ₽?",
                    options = listOf(
                        "Да",
                        "Нет, денег не хватает",
                        "Да, останется 50 ₽",
                        "Цена не имеет значения",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Зачем следить за расходами?",
                    options = listOf(
                        "Чтобы быстрее всё потратить",
                        "Чтобы понимать, куда уходят деньги",
                        "Чтобы цены стали ниже",
                        "Чтобы денег всегда было одинаково",
                    ),
                    correctOptionIndex = 1,
                ),
            ),
        ),
        LearningTestDefinition(
            id = "smart-shopping",
            title = "Покупаем с умом",
            difficulty = LearningTestDifficulty.SIMPLE,
            questions = listOf(
                LearningTestQuestion(
                    prompt = "В двух магазинах одинаковый сок стоит 90 ₽ и 75 ₽. Где дешевле?",
                    options = listOf(
                        "За 90 ₽",
                        "За 75 ₽",
                        "Цена одинаковая",
                        "Нельзя определить",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Что лучше сделать перед покупкой дорогой вещи?",
                    options = listOf(
                        "Купить сразу",
                        "Сравнить цены и подумать, нужна ли она",
                        "Взять самую яркую",
                        "Потратить все деньги",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "У тебя 150 ₽. Ты хочешь купить воду за 50 ₽ и игрушку за 130 ₽. На обе покупки денег…",
                    options = listOf(
                        "Хватит",
                        "Не хватит",
                        "Хватит и останется 30 ₽",
                        "Останется 50 ₽",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Товар со скидкой всегда выгодная покупка?",
                    options = listOf(
                        "Да",
                        "Нет, нужно смотреть цену и нужна ли тебе вещь",
                        "Да, если скидка яркая",
                        "Да, если товар последний",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Какой вопрос полезно задать себе перед незапланированной покупкой?",
                    options = listOf(
                        "Какого цвета ценник?",
                        "Мне это действительно нужно?",
                        "Кто ещё это купил?",
                        "Можно ли потратить ещё больше?",
                    ),
                    correctOptionIndex = 1,
                ),
            ),
        ),
        LearningTestDefinition(
            id = "saving-goal",
            title = "Копим на мечту",
            difficulty = LearningTestDifficulty.SIMPLE,
            questions = listOf(
                LearningTestQuestion(
                    prompt = "Что такое финансовая цель?",
                    options = listOf(
                        "Любая покупка",
                        "То, на что ты решил накопить деньги",
                        "Количество монет",
                        "Скидка в магазине",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Ты хочешь вещь за 500 ₽ и уже накопил 200 ₽. Сколько осталось накопить?",
                    options = listOf(
                        "200 ₽",
                        "300 ₽",
                        "500 ₽",
                        "700 ₽",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Что поможет быстрее накопить?",
                    options = listOf(
                        "Тратить всё сразу",
                        "Регулярно откладывать часть денег",
                        "Не считать деньги",
                        "Каждый день менять цель",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Ты получаешь по 100 ₽ в неделю и откладываешь по 50 ₽. Сколько накопишь за 4 недели?",
                    options = listOf(
                        "50 ₽",
                        "100 ₽",
                        "200 ₽",
                        "400 ₽",
                    ),
                    correctOptionIndex = 2,
                ),
                LearningTestQuestion(
                    prompt = "Зачем нужна копилка?",
                    options = listOf(
                        "Чтобы отделять накопления от денег на текущие траты",
                        "Чтобы деньги исчезали",
                        "Чтобы товары становились дешевле",
                        "Только для хранения монет",
                    ),
                    correctOptionIndex = 0,
                ),
            ),
        ),
        LearningTestDefinition(
            id = "weekly-plan",
            title = "Планируем неделю",
            difficulty = LearningTestDifficulty.MEDIUM,
            questions = listOf(
                LearningTestQuestion(
                    prompt = "У тебя 700 ₽ на неделю. Что разумнее сделать сначала?",
                    options = listOf(
                        "Купить игрушку",
                        "Спланировать основные расходы",
                        "Потратить половину",
                        "Выбрать самый дорогой товар",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Зачем составлять план расходов?",
                    options = listOf(
                        "Чтобы обязательно потратить всё",
                        "Чтобы заранее распределить деньги",
                        "Чтобы увеличить цены",
                        "Чтобы не копить",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "На неделю есть 500 ₽. Ты запланировал потратить 350 ₽. Сколько можно оставить?",
                    options = listOf(
                        "50 ₽",
                        "100 ₽",
                        "150 ₽",
                        "850 ₽",
                    ),
                    correctOptionIndex = 2,
                ),
                LearningTestQuestion(
                    prompt = "Расходы оказались больше запланированных. Что полезно сделать?",
                    options = listOf(
                        "Не обращать внимания",
                        "Посмотреть, на что ушли лишние деньги",
                        "Потратить остаток",
                        "Перестать считать расходы",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Почему не стоит тратить весь недельный бюджет в первый день?",
                    options = listOf(
                        "Денег может не хватить на следующие дни",
                        "Деньги испортятся",
                        "Магазины перестанут работать",
                        "Цены обязательно снизятся",
                    ),
                    correctOptionIndex = 0,
                ),
            ),
        ),
        LearningTestDefinition(
            id = "needs-and-wants",
            title = "Хочу или нужно?",
            difficulty = LearningTestDifficulty.MEDIUM,
            questions = listOf(
                LearningTestQuestion(
                    prompt = "Что скорее относится к необходимым расходам?",
                    options = listOf(
                        "Новая игровая наклейка",
                        "Еда",
                        "Вторая игрушка",
                        "Украшение",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "У тебя осталось 150 ₽ до конца недели. Что важнее купить?",
                    options = listOf(
                        "Украшение за 150 ₽",
                        "Необходимую еду за 100 ₽",
                        "Случайный подарок",
                        "Всё потратить сейчас",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Желание — это…",
                    options = listOf(
                        "То, без чего всегда невозможно обойтись",
                        "То, что хочется получить, но что не всегда необходимо",
                        "Любой доход",
                        "Любые накопления",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Финни голоден, но ты хочешь купить украшение. Денег хватает только на что-то одно. Что разумнее?",
                    options = listOf(
                        "Сначала купить еду",
                        "Купить украшение",
                        "Ничего не покупать",
                        "Выбрать случайно",
                    ),
                    correctOptionIndex = 0,
                ),
                LearningTestQuestion(
                    prompt = "Можно ли иногда тратить деньги на желания?",
                    options = listOf(
                        "Да, если основные расходы учтены",
                        "Никогда",
                        "Только если потратить всё",
                        "Только в первый день недели",
                    ),
                    correctOptionIndex = 0,
                ),
            ),
        ),
        LearningTestDefinition(
            id = "discounts",
            title = "Скидки и акции",
            difficulty = LearningTestDifficulty.MEDIUM,
            questions = listOf(
                LearningTestQuestion(
                    prompt = "Товар стоил 200 ₽, а теперь 150 ₽. Сколько составляет экономия?",
                    options = listOf(
                        "25 ₽",
                        "50 ₽",
                        "150 ₽",
                        "350 ₽",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "В магазине акция «2+1 бесплатно». Когда она действительно полезна?",
                    options = listOf(
                        "Всегда",
                        "Когда тебе действительно нужно три товара",
                        "Только из-за яркого стикера",
                        "Когда товар самый дорогой",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Товар А стоит 80 ₽ без скидки, товар Б — 100 ₽ после скидки. Какой дешевле?",
                    options = listOf(
                        "Товар А",
                        "Товар Б",
                        "Одинаково",
                        "Тот, у которого есть скидка",
                    ),
                    correctOptionIndex = 0,
                ),
                LearningTestQuestion(
                    prompt = "Почему большая скидка не всегда означает выгодную покупку?",
                    options = listOf(
                        "Скидки запрещены",
                        "Товар может быть тебе не нужен",
                        "Цена после скидки всегда выше",
                        "За скидку нужно платить отдельно",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "На что важнее смотреть при сравнении двух одинаковых товаров?",
                    options = listOf(
                        "На цвет ценника",
                        "На размер надписи «АКЦИЯ»",
                        "На итоговую цену",
                        "На расположение на полке",
                    ),
                    correctOptionIndex = 2,
                ),
            ),
        ),
        LearningTestDefinition(
            id = "card-money",
            title = "Деньги на карте",
            difficulty = LearningTestDifficulty.MEDIUM,
            questions = listOf(
                LearningTestQuestion(
                    prompt = "Если деньги находятся на банковской карте, они…",
                    options = listOf(
                        "Ненастоящие",
                        "Всё равно являются деньгами",
                        "Бесконечные",
                        "Не могут закончиться",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Что позволяет увидеть история покупок?",
                    options = listOf(
                        "Будущие цены",
                        "На что были потрачены деньги",
                        "Чужие покупки",
                        "Сколько денег появится завтра",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "На карте 500 ₽. После покупки на 120 ₽ останется…",
                    options = listOf(
                        "120 ₽",
                        "320 ₽",
                        "380 ₽",
                        "620 ₽",
                    ),
                    correctOptionIndex = 2,
                ),
                LearningTestQuestion(
                    prompt = "Что делать, если карта потерялась?",
                    options = listOf(
                        "Ничего",
                        "Сообщить взрослому и заблокировать карту",
                        "Опубликовать её данные",
                        "Ждать несколько недель",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Можно ли сообщать незнакомцу данные банковской карты?",
                    options = listOf(
                        "Да",
                        "Только в игре",
                        "Нет",
                        "Да, если он вежливо попросил",
                    ),
                    correctOptionIndex = 2,
                ),
            ),
        ),
        LearningTestDefinition(
            id = "scams",
            title = "Осторожно, мошенники!",
            difficulty = LearningTestDifficulty.HARD,
            questions = listOf(
                LearningTestQuestion(
                    prompt = "Незнакомец пишет: «Ты выиграл приз! Пришли данные карты». Что делать?",
                    options = listOf(
                        "Отправить",
                        "Отправить только часть",
                        "Не отправлять данные и рассказать взрослому",
                        "Попросить приз подороже",
                    ),
                    correctOptionIndex = 2,
                ),
                LearningTestQuestion(
                    prompt = "Тебе пришёл код подтверждения из банка. Незнакомец просит назвать его. Что делать?",
                    options = listOf(
                        "Назвать",
                        "Никому не сообщать",
                        "Опубликовать",
                        "Переслать друзьям",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Почему мошенник может торопить: «Сделай это прямо сейчас!»?",
                    options = listOf(
                        "Он хочет помочь считать деньги",
                        "Чтобы человек не успел спокойно подумать",
                        "Потому что банк закрывается",
                        "Чтобы снизить цену",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Ты заметил неизвестное списание с карты. Что нужно сделать?",
                    options = listOf(
                        "Подождать неделю",
                        "Сразу сообщить взрослому и обратиться в банк",
                        "Купить что-нибудь ещё",
                        "Удалить приложение банка",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Незнакомый человек знает твоё имя. Значит ли это, что ему можно доверять?",
                    options = listOf(
                        "Да",
                        "Нет",
                        "Да, если он знает фамилию",
                        "Да, если у него красивая фотография",
                    ),
                    correctOptionIndex = 1,
                ),
            ),
        ),
        LearningTestDefinition(
            id = "financial-plan",
            title = "Финансовый план",
            difficulty = LearningTestDifficulty.HARD,
            questions = listOf(
                LearningTestQuestion(
                    prompt = "Ты получил 600 ₽. На обязательные покупки нужно 350 ₽, а 100 ₽ хочешь отложить. Сколько останется на другие траты?",
                    options = listOf(
                        "50 ₽",
                        "100 ₽",
                        "150 ₽",
                        "250 ₽",
                    ),
                    correctOptionIndex = 2,
                ),
                LearningTestQuestion(
                    prompt = "План: потратить 400 ₽. Фактически потрачено 470 ₽. На сколько превышен план?",
                    options = listOf(
                        "30 ₽",
                        "50 ₽",
                        "70 ₽",
                        "870 ₽",
                    ),
                    correctOptionIndex = 2,
                ),
                LearningTestQuestion(
                    prompt = "Что означает, что расходы превышают доходы?",
                    options = listOf(
                        "Появились дополнительные деньги",
                        "Потрачено больше, чем получено",
                        "Ничего не потрачено",
                        "Все деньги отложены",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Что полезнее сделать после окончания недели?",
                    options = listOf(
                        "Забыть обо всех покупках",
                        "Сравнить план с реальными расходами",
                        "Потратить остаток как можно быстрее",
                        "Составить список самых дорогих товаров",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Ты постоянно тратишь больше запланированного на сладости. Что поможет следующему плану?",
                    options = listOf(
                        "Увеличить все расходы",
                        "Учесть реальные траты и решить, сколько готов тратить на сладости",
                        "Перестать вести план",
                        "Не смотреть историю покупок",
                    ),
                    correctOptionIndex = 1,
                ),
            ),
        ),
        LearningTestDefinition(
            id = "financial-master",
            title = "Финансовый мастер",
            difficulty = LearningTestDifficulty.HARD,
            questions = listOf(
                LearningTestQuestion(
                    prompt = "У тебя 500 ₽. Нужно купить еду за 180 ₽, а ещё ты хочешь отложить 150 ₽. Сколько максимум можно потратить на развлечения?",
                    options = listOf(
                        "150 ₽",
                        "170 ₽",
                        "320 ₽",
                        "350 ₽",
                    ),
                    correctOptionIndex = 1,
                ),
                LearningTestQuestion(
                    prompt = "Ты копишь 600 ₽. Уже есть 240 ₽. Если каждую неделю откладывать по 90 ₽, через сколько недель достигнешь цели?",
                    options = listOf(
                        "2",
                        "3",
                        "4",
                        "6",
                    ),
                    correctOptionIndex = 2,
                ),
                LearningTestQuestion(
                    prompt = "Игра стоит 300 ₽. По акции она стоит 250 ₽, но покупать её ты не собирался. Что разумнее всего сделать?",
                    options = listOf(
                        "Купить, потому что есть скидка",
                        "Купить две",
                        "Сначала решить, нужна ли она и вписывается ли покупка в бюджет",
                        "Потратить на неё накопления в любом случае",
                    ),
                    correctOptionIndex = 2,
                ),
                LearningTestQuestion(
                    prompt = "Тебе предлагают подарок за 1000 ₽, но просят назвать код из сообщения банка. Что делать?",
                    options = listOf(
                        "Назвать код ради подарка",
                        "Назвать половину кода",
                        "Не сообщать код и рассказать взрослому",
                        "Сначала спросить, можно ли получить два подарка",
                    ),
                    correctOptionIndex = 2,
                ),
                LearningTestQuestion(
                    prompt = "В конце недели у тебя осталось 100 ₽. Что можно сделать?",
                    options = listOf(
                        "Обязательно сразу потратить",
                        "Выбросить",
                        "Оставить или добавить к накоплениям",
                        "Купить что угодно, лишь бы баланс стал нулевым",
                    ),
                    correctOptionIndex = 2,
                ),
            ),
        ),
    )

    val byId = tests.associateBy(LearningTestDefinition::id)
}
