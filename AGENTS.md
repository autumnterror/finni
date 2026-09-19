# AGENTS.md

## 1. Purpose

This file is the shared product, architecture, design, educational, and integration contract for all developers and coding agents working on the project.

The project is developed feature-by-feature and AI-first. A developer or agent may work on one feature without knowing the implementation details of other in-progress features. Therefore:

1. read this file before making non-trivial changes;
2. inspect existing feature documentation and shared contracts;
3. keep feature ownership explicit;
4. reuse shared domain and design-system primitives;
5. avoid hidden coupling and duplicate global logic;
6. create or update module/feature documentation only when explicitly requested; keep approved shared rules in this contract consistent.

This file is aligned with **MVP 6.2**.

If a newer approved specification conflicts with this file:
- a feature-local specification may override this file only for that feature;
- if the change affects the weekly loop, economy, pet state, educational model, XP, persistence, architecture, or shared UX, update this file in the same change.

---

## 2. Current Product Context

### 2.1. Product

The product is an Android / RuStore mobile game for children aged **7-11**.

The player creates a virtual pet, receives limited pocket money, plans a weekly budget, cares for the pet, buys items, saves for goals, completes financial situations, may perform a limited amount of side work, and sees the consequences of financial decisions.

The product must feel like a **virtual-pet game with financial decisions**, not like a finance dashboard or quiz app.

### 2.2. Educational basis

The educational basis is the **Unified Framework of Financial Literacy Competencies**, for children aged 7-11.

Educational progress is represented through achievements earned from meaningful
financial actions. Every achievement has a stable child-facing definition and a
separate plain-language sentence for the parent section.

### 2.3. Canonical MVP loop

```text
Create pet
↓
Start week
↓
Receive pocket money
↓
Create weekly financial plan
↓
Monday
actions / purchases / events / feeding
↓
Bed → End day
↓
Tuesday → ... → Sunday
↓
If needed:
limited side job / contextual debt
↓
End week
↓
Plan ↔ Actual
↓
Neutral learning feedback
↓
XP / pet progression
↓
Next week
```

The **game week** is the main product unit.

The **game day** is an internal unit used to pace needs, events, known future expenses, messages, and player decisions.

---

## 3. MVP Priority Rule

The project must first deliver a complete, stable vertical slice of the required contest flow.

Do not delay the mandatory weekly loop to build optional content.

Priority order:

### P0 — mandatory product path

1. local guest profile;
2. pet creation and basic customization;
3. main apartment screen;
4. game day and bed;
5. economy foundation;
6. pocket money;
7. weekly planning;
8. purchases;
9. savings and financial goal;
10. feeding;
11. financial tasks/events;
12. Plan ↔ Actual weekly result;
13. learning feedback;
14. persistence;
15. demo mode;
16. adult/parent section;
17. three visible pet growth stages.

### P1 — competitive additions

1. happiness;
2. premium food as optional comfort;
3. achievement-based educational progress with parent-facing mapping;
4. limited side jobs;
5. debt only for an important purchase;
6. early end of an unrecoverable crisis week;
7. XP;
8. 1-2 mini-games;
9. digital financial safety.

### P2 — only after P0/P1 is stable

- expanded customization;
- more mini-games;
- more room zones;
- complex adaptive event scheduling;
- long history and analytics;
- deeper story;
- large content catalog.

---

## 4. Non-Negotiable Product Principles

### 4.1. The pet is the emotional center

The application is a virtual-pet game first.

The pet must not become a decorative mascot attached to a finance UI.

Whenever possible:
- actions happen around the pet;
- room objects have clear pet/game meaning;
- purchases visibly affect the pet or room;
- progression changes the pet visually or behaviorally.

### 4.2. The apartment is the main hub

MVP apartment objects:

- **phone** — shop, messages, short transaction history;
- **notebook** — financial tasks/tests and limited paid side work;
- **piggy bank** — savings goal and transfers;
- **wardrobe** — customization;
- **fridge** — food inventory;
- **table** — feeding;
- **bed** — end current game day.

Do not reintroduce old room metaphors such as calendar/task-board/bowl as primary MVP objects unless an approved newer design explicitly requires them.

### 4.3. Teaching happens through decisions and consequences

Preferred loop:

```text
Situation
↓
Useful information
↓
Player choice/action
↓
Financial and game consequence
↓
Short explanation
↓
Later repetition in another context
```

Avoid turning the product into:

```text
Question
→ Correct answer
→ XP
→ Next question
```

Tests are allowed, but they are only one possible learning action and must not
replace decisions inside the weekly game loop.

### 4.4. Mistakes teach; they do not punish progression

Do not use:
- negative XP;
- player-level loss;
- permanent educational-progress regression;
- deletion of purchased items;
- shaming language;
- pet death;
- severe punishment for a single mistake.

Use:
- consequence;
- explanation;
- later repetition;
- a recoverable next step.

### 4.5. The player may make a bad financial choice

Do not automatically block every poor decision.

If the player can afford an optional purchase, the game may allow it even if the decision is unwise.

The UI must show enough information to make the consequence understandable.

### 4.6. No real-money recovery loop

Do not implement:
- real-money currency purchases;
- paid recovery;
- required rewarded ads;
- pay-to-win educational progression.

---

## 5. Weekly Planning

Weekly planning is **P0** and part of the core loop.

Before the active week begins, the player distributes available money across at least:

- mandatory expenses;
- optional expenses;
- savings.

A free remainder acts as the financial reserve and must also be displayed. It may
be represented as the share left after the three planned categories rather than
as a fourth spending category.

Example:

```text
Available: 500

Mandatory      200
Wants          150
Savings        100
Financial reserve / free remainder  50
```

Rules:
- planned total must not exceed available money;
- the plan can be edited before confirmation;
- after confirmation it is fixed for that week;
- the plan does **not** hard-block later actions;
- actual spending may differ;
- the end-of-week screen compares Plan ↔ Actual.

The system must evaluate the **reason** for deviation, not only numerical equality.

Possible internal categories:
- reasonable deviation;
- conscious deviation;
- impulsive deviation.

Do not show these technical labels to the child.

---

## 6. Game Day and Bed

### 6.1. Day model

A week contains seven game days:

```text
Monday
Tuesday
Wednesday
Thursday
Friday
Saturday
Sunday
```

The day exists to:
- pace hunger;
- schedule events;
- surface messages/tasks;
- make known future expenses meaningful;
- create a reason not to spend the entire weekly budget immediately.

There is no mandatory fixed number of actions per day.

### 6.2. Bed behavior

The bed action is:

> **End day**

When the player confirms:
- advance to the next day;
- apply need changes;
- advance scheduled events;
- activate new messages/tasks if eligible;
- record the day state.

The bed must **not**:
- restore money;
- issue new pocket money before the week ends;
- cancel unresolved consequences;
- reset pet needs.

If an unresolved mandatory event requires a decision, it must be resolved before ending the day.

If hunger is low, the UI may warn the player before ending the day, but the player may continue.

Demo mode must allow fast day advancement with no real-time waiting.

---

## 7. Pet State

### 7.1. Hunger

Hunger is the mandatory care need.

Suggested domain range:

```text
0..100
```

Rules:
- feeding raises hunger/satiety;
- advancing the day may lower it;
- the exact decay is configurable;
- the pet does not die;
- low hunger does not delete progression.

### 7.2. Happiness

Happiness is a competitive addition and represents optional comfort, play, and variety.

It must **not** teach:

```text
more spending = more love = happier pet
```

Meaningful free sources must exist:
- pet interaction;
- already unlocked mini-games;
- play;
- normal care.

Optional purchases may add happiness.

Refusing an optional purchase must not automatically reduce happiness.

### 7.3. Food tiers

Basic food fully covers the mandatory need.

More expensive food may provide the same hunger restoration plus a small happiness bonus.

Example configuration:

```text
Basic food       60 → +40 hunger, +0 happiness
Tasty food       80 → +40 hunger, +3 happiness
Favorite meal   110 → +40 hunger, +6 happiness
3 basic portions 150 → 3 × +40 hunger
```

The purpose is to create a clear **need vs optional comfort** decision.

Exact values are configuration, not hardcoded product truth.

---

## 8. Economy Invariants

Economy is shared critical infrastructure.

### 8.1. Single transaction path

Every money change must create a transaction record.

Examples:
- weekly pocket money;
- purchase;
- savings transfer;
- side-job reward;
- debt;
- debt repayment;
- event consequence.

Do not mutate balances directly inside feature UI.

### 8.2. No negative spendable balance

A purchase must not produce a negative balance.

If funds are insufficient:
- do not complete the purchase;
- explain the shortfall;
- present allowed alternatives where relevant.

### 8.3. Idempotency

Double taps, recomposition, repeated callbacks, restoration, or navigation races must not charge/reward twice.

Use stable operation/transaction IDs or equivalent safeguards.

### 8.4. Pocket money

Pocket money is regular weekly income.

It is granted at the start of the next week.

The amount may increase with player level/configuration.

Do not make all mandatory expenses increase linearly with income.

Higher progression should primarily open:
- more wants;
- higher-priced optional goods;
- larger savings goals;
- more content and choices.

### 8.5. Planning data

The economy domain must support:
- weekly starting balance;
- weekly allowance;
- planned mandatory amount;
- planned optional amount;
- planned savings amount;
- free remainder;
- actual categorized spending;
- weekly income/side-income;
- debt impact;
- end-of-week summary.

---

## 9. Savings

Savings are represented through the piggy bank.

MVP rules:
- wallet and savings are separate;
- player selects a savings goal;
- at least 3 goals exist in demo content;
- show target amount;
- show saved amount;
- show remaining amount;
- allow transfer to savings;
- withdrawal requires explicit confirmation.

The game may show an estimated time to goal if calculation is simple and transparent.

Meaningful savings actions can advance achievement progress.

Opening the savings screen does not advance achievement progress.

---

## 10. Side Jobs

Side jobs are a limited recovery/income mechanism.

MVP examples:
- help grandmother;
- paid notebook task.

Rules:
- paid opportunities are limited per week;
- side jobs must not make weekly scarcity meaningless;
- mini-games are not an infinite currency faucet;
- tests are not a constant money-farming loop.

---

## 11. Debt

Debt is an age-appropriate family mechanic, not a bank credit simulator.

Preferred wording:

> Ask parents for money until the next pocket-money payment.

Debt is **contextual**, not a permanently available generic money button.

Show it only when:
- the player cannot afford an important allowed purchase;
- the expense category explicitly allows debt;
- there is no conflicting active debt rule.

Rules:
- at most one active debt;
- debt amount is capped;
- debt reduces the next pocket-money payment;
- no new debt until the current one is repaid;
- debt cannot be used for cosmetics, room luxuries, or optional wants;
- no interest in MVP.

Before confirmation show:
- amount received now;
- amount to repay;
- effect on next allowance.

---

## 12. Financial Crisis / Early Week End

If all are true:
- the pet is hungry;
- there is no food;
- current money is insufficient;
- paid side jobs are exhausted;
- debt is unavailable or insufficient;
- no allowed recovery path remains;

the week may end early.

Use neutral copy such as:

> There was not enough money this week. The parents helped take care of the pet. Next time, try to leave some money for necessary expenses.

After crisis resolution:
- the pet receives required care;
- progress is not deleted;
- XP is not reduced;
- unlocked achievements and accumulated achievement progress are not reduced;
- purchased items remain;
- the week is recorded as an early finish.

---

## 13. Shop and Purchases

MVP must include at least 8 purchase positions across mandatory and optional categories.

Before purchase show:
- item;
- price;
- category;
- expected pet/game effect;
- current balance;
- balance after purchase.

Useful optional context:
- current hunger;
- savings goal impact;
- remaining days in week;
- debt impact if relevant.

Purchase confirmation is required.

---

## 14. Financial Tasks and Events

MVP must contain at least 6 educational tasks across at least 3 themes:

### Budget planning
- not enough money for everything;
- known future mandatory expense.

### Savings
- choose how much to save;
- decide whether to withdraw savings for an optional purchase.

### Payments and purchases
- compare two packages;
- check purchase/discount/change.

Competitive safety additions:
- suspicious message;
- unknown link / request for confirmation code.

Events should be data-driven where practical.

Suggested event shape:

```text
eventId
source
type
conditions
content
choices/actions
consequences
feedback
learningActionType
learningContext
repeatGroup
dayEligibility
```

Do not build every event as a separate one-off screen.

---

## 15. Achievement-Based Educational Model

### 15.1. Learning loop and system boundary

Educational progress follows this loop:

```text
Meaningful financial action
↓
Game consequence
↓
Short feedback from the pet
↓
Achievement progress or unlock
↓
Plain-language progress in the parent section
```

Achievements show which financial actions the child has encountered and learned
to apply. XP shows game and pet progression. They are separate systems, even
though every first achievement unlock grants a configured amount of XP.

Opening a screen, reading a hint, recomposition, or repeating the same operation
ID is not a qualifying financial action. Mistakes may trigger explanation and a
future repeat, but never remove progress or an unlocked achievement.

### 15.2. Topics, metrics, points, and achievements

The current MVP catalog contains four learning topics:

```text
budget_planning
savings_building
payments_and_purchases
financial_security
```

Each topic contains concrete metrics. A normal metric has two progress points and
two achievements:

1. **Introduction** — the child first encounters or performs the action, usually
   with an explanation from the pet.
2. **Learned** — the child repeats the action, performs it independently, or
   applies it in changed conditions according to that metric's rule.

Application in changed conditions is a qualifying path to the second achievement,
not a third generic achievement. Thresholds such as the number of weeks or repeats
are configuration and must not be scattered through feature UI code.

Financial-security metrics are the deliberate exception. Each has three progress
steps but still only two achievements:

1. guided suspicious situation → Introduction achievement;
2. independent safe response;
3. safe response reinforced in a later situation → Learned achievement.

A topic is considered completed when the second achievement is unlocked for all
of its active metrics. Topic completion may be shown as a visual frame or badge,
but it does not replace the individual achievements or their parent-facing rows.

### 15.3. Current metric catalog and parent mapping

Every achievement definition must contain a stable parent-facing sentence. Each
unlocked achievement creates its own row in the parent section; a topic-level
summary must not hide or replace these rows.

#### Budget planning

| Metric | Introduction row | Learned row |
|---|---|---|
| `reasonable_plan` | Ребёнок ознакомился с распределением денег между обязательными расходами, желаниями, сбережениями и финансовой подушкой. | Ребёнок научился составлять недельный план, в котором хватает денег на обязательные расходы. |
| `follow_plan` | Ребёнок ознакомился со сравнением недельного плана с фактическими расходами. | Ребёнок научился следовать недельному плану на протяжении нескольких игровых периодов. |
| `adapt_to_change` | Ребёнок ознакомился с тем, как неожиданный расход, дополнительный доход или изменение цены влияет на бюджет. | Ребёнок научился адаптировать оставшиеся расходы к изменившейся финансовой ситуации. |

The Introduction achievement for `reasonable_plan` may be granted after the first
completed planning attempt and explanation, even when the plan is weak. The
Learned achievement requires an adequate plan. A confirmed weekly plan remains
fixed; adaptation means changing later decisions and the use of remaining money,
not silently rewriting the confirmed plan.

At the end of a period, feedback evaluates plan quality separately from adherence:

- adequate plan + small justified deviation: positive explanation;
- adequate plan + poor adherence: explain what changed and how to improve;
- weak plan + close adherence: acknowledge consistency while explaining why the
  original plan was risky.

After the introductory cycles, adaptation may be checked through a known future
expense, extra income, an unexpected mandatory expense, or a configured temporary
price change. A price-change scenario is not mandatory until its rules and content
are approved.

#### Savings building

| Metric | Introduction row | Learned row |
|---|---|---|
| `create_goal` | Ребёнок ознакомился с созданием финансовой цели. | Ребёнок научился самостоятельно выбирать и создавать достижимую финансовую цель. |
| `plan_saving` | Ребёнок ознакомился с включением сбережений в недельный план. | Ребёнок научился планировать регулярные пополнения финансовой цели. |
| `regular_contribution` | Ребёнок ознакомился с откладыванием денег на финансовую цель. | Ребёнок научился пополнять финансовую цель на протяжении нескольких игровых периодов. |
| `reach_goal` | Ребёнок ознакомился с завершением накопления на выбранную цель. | Ребёнок научился доводить план накопления до достижения цели. |

The piggy-bank introduction explains its purpose but does not itself advance a
metric. Creating a goal, confirming a plan with savings, a successful transfer,
and reaching a target are separate learning actions. Friendly reminders are
allowed; the pet must not guilt the child or imply that affection depends on
buying the desired item.

#### Payments and purchases

| Metric | Introduction row | Learned row |
|---|---|---|
| `reasonable_purchase` | Ребёнок ознакомился с проверкой того, подходит ли покупка текущему бюджету и обязательным потребностям. | Ребёнок научился оценивать покупку с учётом доступных денег и предстоящих обязательных расходов. |
| `promotion_decision` | Ребёнок ознакомился с проверкой реальной пользы акции. | Ребёнок научился оценивать акции с учётом цены, необходимости покупки и доступных денег. |
| `impulse_decision` | Ребёнок ознакомился с паузой перед импульсивной покупкой. | Ребёнок научился оценивать импульсивное желание, не ставя под угрозу обязательные расходы. |

A choice is not judged only by whether the child bought or refused an item. The
rule must consider mandatory needs, basket contents, current and planned money,
remaining days, and the consequence of the choice. Receipt checking is a deferred
metric and must not be added to the active catalog until a receipt mechanic exists.

#### Financial security

| Metric | Introduction row | Learned row |
|---|---|---|
| `confirmation_code_request` | Ребёнок ознакомился с тем, почему просьба сообщить код подтверждения подозрительна. | Ребёнок научился не сообщать коды подтверждения и обращаться за помощью к взрослому. |
| `unknown_link` | Ребёнок ознакомился с тем, почему неизвестная ссылка может быть опасна. | Ребёнок научился не переходить по неизвестным ссылкам и самостоятельно выбирать безопасную реакцию. |

On the first occurrence, the pet points out that something looks suspicious and
explains the nature of the risk. The second occurrence checks an independent
choice; the third reinforces it in a later or changed situation. Only the first
and third steps unlock achievements.

### 15.4. Required learning-module contracts

The learning module owns:

- the achievement catalog and thresholds;
- mapping successful game actions to metric progress;
- idempotent processing of learning actions;
- accumulated metric progress and unlocked achievements;
- first-time explanation state;
- the mapping from every achievement to its parent-facing sentence;
- an idempotent request to grant XP for each first unlock.

Source features own the truth about what happened. Planning, Savings, Shop,
Events, and Week Summary report immutable successful outcomes with a stable
`learningActionId`; they do not increment achievement counters or write learning
tables directly. The learning module evaluates those facts centrally.

Minimum equivalent domain concepts:

```text
LearningAction
  actionId
  actionType
  weekId / game period
  sourceOperationId?
  immutable context required by the rule

AchievementDefinition
  achievementId
  topicId
  metricId
  stage: INTRODUCTION | LEARNED
  threshold/rule
  xpReward
  parentText

MetricProgress
  metricId
  progressSteps
  qualifyingRepeats / distinct periods where required

AchievementUnlock
  achievementId
  sourceActionId
  unlockedAtGameTime
  xpGrantId
```

The public API must at minimum support idempotent action recording, observing
unlocked achievements, observing parent progress rows, and checking/marking
first-time explanations. A generic application-wide event bus is not required for
MVP; direct feature contracts and coordinators are sufficient.

A committed qualifying game action must not lose its learning action after process
death. Use the same shared Room transaction when practical, or persist a small
source outbox with the domain outcome and retry `LearningApi.record`. A
fire-and-forget callback after commit is not sufficient.

Achievement unlock and XP delivery must survive process death without duplication.
Use the stable grant ID derived from the profile and achievement, or an equivalent
transactional outbox. Reprocessing the same learning action returns the existing
result and neither advances progress nor grants XP again.

The detailed current technical contract lives in `docs/features/learning.md`.

---

## 16. XP and Pet Progression

XP is a **game progression system**, not the educational progress store.

Achievements and XP are separate systems. Every achievement grants XP exactly
once when it is first unlocked, using a stable grant ID. The achievement remains
the educational record; the XP amount must not be used to infer which financial
action the child learned.

Primary XP sources may include:
- first achievement unlocks;
- completed week;
- achieved financial goal;
- key game task;
- opened content.

Mini-games may award a small amount of XP.

MVP pet progression needs at least 3 visible stages:
- baby;
- explorer;
- companion.

Pet growth must be explainable by a series of financial/game decisions across multiple weeks.

Pet growth must not be based on:
- total money spent;
- buying the most expensive items;
- one repeated mini-game farm.

---

## 17. Mini-Games

For MVP, 1-2 simple mini-games are enough.

Examples:
- ball;
- drawing.

Rules:
- short;
- one-finger where possible;
- pet is involved;
- fast restart;
- may give happiness or small XP;
- not an infinite source of money.

---

## 18. Parent / Adult Section

The adult section is mandatory.

Protect it with a simple child barrier such as:
- hold action;
- simple arithmetic prompt.

Show:
- product goals;
- covered themes;
- overall progress;
- one plain-language row for every unlocked achievement;
- topic-level progress derived from the achievement catalog;
- qualitative observations.

Do not show judgmental labels.

Preferred:

```text
Budget planning
Ребёнок ознакомился с составлением недельного плана.
Ребёнок научился оставлять достаточно денег на обязательные расходы.
```

Avoid:

```text
reasonable_plan = 2/2
child made 4 bad decisions
```

---

## 19. End-of-Week Result

At week end show:

1. Plan;
2. Actual;
3. short explanation of what changed;
4. one positive observation;
5. one next-step suggestion.

Do not use school grades.

Do not evaluate only numeric equality.

The result should distinguish between:
- necessary deviation;
- intentional tradeoff;
- impulsive overspending.

The copy shown to the child remains simple and neutral.

---

## 20. Demo Mode

Demo mode is mandatory and must be deterministic enough for a live jury demonstration.

It must support:
- at least 5 sequential game weeks;
- no real-time waiting;
- fast day advancement through bed;
- required educational situations;
- insufficient-funds case;
- savings flow;
- Plan ↔ Actual result;
- visible pet growth stage change;
- test-profile reset.

Debug/demo shortcuts must not accidentally ship as normal player behavior unless intentionally designed.

---

## 21. Persistence

Persist enough state to restore a deterministic game state.

At minimum:

- profile;
- pet identity/customization;
- hunger;
- happiness if enabled;
- current week;
- current day;
- wallet balance;
- transactions;
- current allowance;
- food inventory;
- purchases;
- savings;
- savings goal;
- debt;
- current weekly plan;
- current weekly actuals;
- week history required by MVP;
- event history;
- processed learning actions;
- metric progress;
- unlocked achievements;
- first-time learning explanations already shown;
- pending/applied achievement XP grant IDs;
- XP;
- pet stage;
- unlocked mini-games/content.

During the current pre-release development stage, schema changes do not require database migrations. Do not add manual migrations, auto-migrations, or compatibility code for old development schemas unless explicitly requested.

If a local development database needs to be recreated after a schema change, use an explicitly agreed development-only reset. This rule does not authorize deleting existing migration code or silently clearing data, and does not apply automatically to released applications with user data to preserve.

Do not silently reset progress.

---

## 22. Technical Direction

Use the repository's existing Android core package and patterns.

Repository assumption from the previous project contract:

```text
github.detrig.core
```

Before implementing feature modules, DI, navigation, UI/MVVM, storage, or networking, read the corresponding project skills under `.codex/skills` if they exist.

Default direction:

- **Language:** Kotlin
- **Platform:** Android
- **UI:** Jetpack Compose
- **State:** StateFlow
- **Async:** Kotlin Coroutines
- **Navigation:** Navigation 3 / repository wrappers
- **DI:** repository manual DI (`Feature`, `Dependencies`, `Component`, `Module`, `Api`) where currently used
- **Structured local data:** Room
- **Simple preferences:** existing project SharedStorage / SharedPreferences abstraction
- **Build:** Gradle Kotlin DSL unless repository says otherwise

Do not add Hilt, Dagger, or Koin unless explicitly approved.

Do not introduce a full game engine for simple room interactions or mini-games unless a concrete requirement justifies it.

Prefer Compose / Canvas for simple interactive content.

---

## 23. Suggested Domain Boundaries

Exact module names may differ.

Recommended conceptual boundaries:

```text
:app

:core:model
:core:data
:core:database
:core:designsystem
:core:time

:domain:economy
:domain:week
:domain:pet
:domain:learning
:domain:events
:domain:progression

:feature:onboarding
:feature:room
:feature:planning
:feature:phone
:feature:shop
:feature:food
:feature:savings
:feature:notebook
:feature:parent
:feature:weeksummary

:game:core
:game:ball
:game:drawing
```

Do not create modules only for architectural purity.

The important rules are:
- ownership;
- dependency direction;
- stable contracts.

---

## 24. Shared Contract Rules

### 24.1. Features do not mutate each other's internal state

Bad:

```text
Shop UI
→ writes Pet database state
```

Preferred:

```text
Shop
→ Economy purchase contract
→ domain result
→ Pet/Inventory receives explicit domain effect
```

### 24.2. Cross-domain coordination belongs in orchestration

Avoid circular dependencies such as:

```text
Shop → Pet → Shop
Events → Learning → Events
Week → Economy → Week
```

Use an application/domain coordinator for cross-domain flows.

### 24.3. Shared rules live once

Do not duplicate:
- money arithmetic;
- purchase validation;
- transaction recording;
- weekly rollover;
- plan-vs-actual aggregation;
- day advancement;
- hunger change rules;
- debt rules;
- achievement rules and thresholds;
- learning-action idempotency;
- achievement-to-parent-text mapping;
- achievement XP grant IDs;
- XP rules.

### 24.4. UI does not own persistence

Preferred:

```text
Composable
→ ViewModel
→ Use Case
→ Repository
→ Data Source
```

### 24.5. Side effects are explicit

Examples:
- purchase completed;
- food consumed;
- day ended;
- week ended;
- debt created;
- debt repaid;
- event resolved;
- learning action recorded;
- achievement unlocked;
- XP granted;
- pet stage changed.

Do not hide domain side effects in Compose recomposition.

---

## 25. Core Domain Models

Implementations may differ, but the domain must be able to represent equivalent concepts.

### Week

```text
weekId
weekNumber
currentDay
status
startingBalance
allowanceAmount
plan
actuals
earlyFinishReason?
startedAt / game-time marker
completedAt?
```

### WeeklyPlan

```text
mandatoryPlanned
optionalPlanned
savingsPlanned
freeRemainder
confirmed
```

### WeeklyActual

```text
mandatorySpent
optionalSpent
saved
sideIncome
debtReceived
debtRepaid
```

### Debt

```text
debtId
amount
remaining
createdWeek
repayOnNextAllowance
allowedReason
status
```

### Educational progress

```text
achievement catalog version
processed learning action IDs
progress steps by metric
unlocked achievement IDs
first-time explanation flags
achievement XP grant status
```

### XP / Progression

```text
xp
gameLevel
petStage
unlocks
```

Do not mix achievement progress and XP in one numeric field.

---

## 26. Android UI Skill

Before creating or changing visual Compose UI, read `.codex/skills/android-core-compose-ui/SKILL.md`. It owns the detailed rules for design-system components, theme tokens, layout, and previews; do not duplicate those rules in this contract.

For ViewModel, UI-state, events, and commands, use `android-core-ui-mvvm`. DI and navigation remain governed by their respective Android skills.

---

## 27. UX Rules for Children 7-11

- minimum touch target: 48×48 dp;
- prefer 56×56 dp for primary actions;
- keep primary copy short;
- avoid long educational text on the room screen;
- do not communicate state by color alone;
- show one dominant teaching prompt at a time;
- sound is supplementary;
- no manipulative urgency;
- no guilt from the pet;
- no dark patterns.

The UI must make important consequences visible before confirmation.

---

## 28. Main Screen Information Hierarchy

The child should be able to quickly understand:

1. current spendable balance;
2. current day of the week;
3. savings / current goal;
4. pet hunger;
5. pet happiness if enabled;
6. current task/important notification;
7. available room interactions.

Do not turn the HUD into a spreadsheet.

Detailed history belongs in the phone or contextual sheets.

---

## 29. Configuration

Values likely to change in balancing must be configurable:

- allowance by level;
- item prices;
- food effects;
- hunger change per day;
- happiness effects;
- side-job limits;
- side-job rewards;
- debt cap;
- debt-allowed categories;
- XP rewards;
- pet-stage thresholds;
- event eligibility;
- day/event schedule;
- savings goals;
- demo content.

Do not scatter balancing numbers through feature UI code.

---

## 30. Testing Priorities

Do not optimize for coverage percentage during MVP development.

Prioritize high-risk domain behavior.

### Must test

- wallet transactions;
- purchase idempotency;
- insufficient funds;
- weekly allowance issuance;
- day advancement;
- no allowance before week rollover;
- plan validation;
- Plan ↔ Actual aggregation;
- savings transfer;
- debt eligibility;
- one-active-debt rule;
- next-allowance debt deduction;
- side-job weekly limits;
- crisis-week condition;
- hunger transitions;
- standard two-step achievement thresholds;
- financial-security three-step/two-achievement thresholds;
- learning-action idempotency;
- complete achievement-to-parent-row mapping;
- first-time explanation shown once;
- one XP grant per first achievement unlock;
- XP grant idempotency;
- pet-stage thresholds;
- persistence/restoration;
- demo reset.

### Important UI tests

- weekly planning;
- purchase confirmation;
- insufficient funds;
- feeding;
- savings transfer;
- end-day confirmation/warning;
- event resolution;
- week summary;
- parent section;
- pet stage change.

---

## 31. Optional Feature Documentation

Creating or updating a module/feature does not require a README, a document under `docs/features/`, or a completion report file. Write or update this documentation only when explicitly requested by the user.

Continue reading existing documentation and shared contracts when relevant. Do
not delete existing documents just because new documentation is optional. When
documentation is requested or an existing feature document needs an update,
prefer this location:

```text
docs/features/<feature_name>.md
```

Use only the relevant parts of this checklist:

- goal;
- educational goal;
- ownership;
- modules;
- domain model;
- dependencies;
- provided contracts;
- UI;
- persistence;
- events;
- learning actions and achievement integration;
- configuration;
- edge cases;
- acceptance criteria.

Existing feature documentation is a living contract and must be updated when an
implementation change affects behavior already described there.

---

## 32. Optional Feature Ownership Template

```markdown
# Feature: <name>

## Goal
...

## Educational Goal
...

## Ownership

This feature owns:
- ...

This feature does not own:
- ...

This feature consumes:
- ...

This feature provides/emits:
- ...

## User Flow
1.
2.
3.

## Domain Model
...

## UI States
- default
- loading
- empty
- insufficient resources
- error
- completed

## Persistence
...

## Events
...

## Achievement Integration
Learning actions emitted:
Metrics affected:
Achievement IDs and thresholds:
First-time explanation and feedback:
Parent-facing rows:
XP reward:

## Configuration
...

## Edge Cases
...

## Acceptance Criteria
...
```

---

## 33. Definition of Done

A feature is not done because the happy-path screen renders.

Before merging, verify:

### Product
- matches MVP 6.2;
- does not contradict the weekly loop;
- handles failure/empty/insufficient-resource states;
- uses age-appropriate copy.

### Architecture
- no duplicate shared logic;
- no circular feature dependency;
- UI does not mutate persistence directly;
- navigation uses repository patterns;
- side effects are explicit.

### Economy
If money is involved:
- transaction recorded;
- duplicate charge/reward prevented;
- insufficient funds handled;
- post-operation balance correct;
- weekly categories updated.

### Education
If educational:
- emits an idempotent learning action only after a real game outcome;
- maps to a metric and achievements in the shared catalog;
- defines the Introduction/Learned threshold;
- provides a parent-facing row for every achievement;
- grants configured XP exactly once per first unlock;
- feedback explains consequence;
- poor choices do not delete prior progress or achievements.

### Time/week
If time is involved:
- current day persists;
- day advancement is deterministic;
- no early allowance leak;
- week rollover is tested.

### Quality
- important domain rules tested;
- repeated taps/navigation do not duplicate side effects;
- no debug shortcuts leak into normal flow;
- Android UI changes follow the relevant skill referenced in section 26;
- module/feature documentation is created or updated only when explicitly requested.

---

## 34. Anti-Patterns

Do not:

- build against the obsolete pre-6.2 continuous-income loop;
- treat pocket money as a free-running real-time timer;
- skip weekly planning;
- hard-block every poor purchase;
- make bed a consequence-free money-farming shortcut;
- make happiness proportional to spending;
- create an unlimited side-job faucet;
- offer debt for cosmetic/optional purchases;
- allow multiple active debts;
- merge XP and achievement progress into one progression number;
- duplicate achievement rules or counters inside feature modules;
- advance learning progress merely for opening a screen or showing a hint;
- award progress twice for the same operation/action ID;
- score motivation/responsibility from one click;
- turn the notebook into the whole game;
- make mini-games the main income source;
- decrease XP or achievement progress after mistakes;
- use shaming copy;
- build a finance-dashboard aesthetic;
- duplicate wallet/plan/week logic inside features;
- store critical state only in memory;
- create a second architecture stack for one feature.

---

## 35. Current Source-of-Truth Order

When documents disagree, use this precedence unless explicitly changed by the project owner:

1. latest explicitly approved feature/product decision in the current task;
2. MVP 6.2 product specification;
3. this `AGENTS.md`;
4. current feature documentation;
5. older Version 6.x / Version 5 documents;
6. implementation assumptions not documented elsewhere.

Older Version 5 mechanics are **not** authoritative when they conflict with MVP 6.2.

---

## 36. Final Integration Principle

Independent feature work must converge into one coherent weekly game.

The shared product spine is:

```text
Pocket money
↓
Plan
↓
7 game days
↓
Pet care + purchases + savings + events
↓
Limited recovery tools
↓
Plan ↔ Actual
↓
Feedback
↓
XP / pet growth
│
└── meaningful learning actions
    ↓
Metric progress
    ↓
Introduction / Learned achievements
    ↓
Parent section
```

When uncertain, prefer consistency with:
- weekly planning;
- shared economy contracts;
- action-based achievement progress;
- explicit achievement-to-parent-row mapping;
- separate, idempotent XP rewards;
- neutral consequences;
- the child-facing virtual-pet experience.
