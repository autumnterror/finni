# AGENTS.md

## 1. Purpose

This file is the shared product, architecture, design, educational, and integration contract for all developers and coding agents working on the project.

The project is developed feature-by-feature and AI-first. A developer or agent may work on one feature without knowing the implementation details of other in-progress features. Therefore:

1. read this file before making non-trivial changes;
2. inspect existing feature documentation and shared contracts;
3. keep feature ownership explicit;
4. reuse shared domain and design-system primitives;
5. avoid hidden coupling and duplicate global logic;
6. update documentation when a change affects shared behavior.

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

Educational progress is represented internally through a hidden SkillId system.

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
3. SkillId with BASIC / ADVANCED levels;
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

Tests are allowed, but they are only one evidence source.

### 4.4. Mistakes teach; they do not punish progression

Do not use:
- negative XP;
- player-level loss;
- permanent skill regression;
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

A free remainder may also be displayed.

Example:

```text
Available: 500

Mandatory      200
Wants          150
Savings        100
Free remainder  50
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

Savings can produce behaviour evidence.

Opening the savings screen is not evidence.

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
- SkillId mastery is not reduced;
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
targetSkillId
frameworkLevel
conditions
content
choices/actions
consequences
feedback
evidenceType
repeatGroup
dayEligibility
```

Do not build every event as a separate one-off screen.

---

## 15. SkillId Educational Model

### 15.1. SkillId is hidden from the child

SkillId is internal educational analytics.

The child should not see:
- technical SkillId names;
- competency scores;
- mastery numbers.

The adult/parent section may show plain-language progress.

### 15.2. MVP SkillId groups

Use these seven high-level competencies:

```text
budget_management
savings
purchase_decision
price_evaluation
debt_management
financial_security
digital_finance
```

Do not create feature-local duplicate IDs.

Do not split one competency into many near-synonymous IDs such as:
- `needs_vs_wants`;
- `prioritize_needs`;
- `limited_budget`;
- `allocate_limited_budget`;

when they are manifestations of `budget_management`.

### 15.3. Framework level

Each SkillId has:

```text
BASIC
ADVANCED
```

This is the **educational result level** from the Framework.

It is not the child's mastery score.

### 15.4. Mastery

Each BASIC/ADVANCED branch may have mastery:

```text
0 = no evidence
1 = introduced
2 = independently applied
3 = reinforced / transferred
```

Example:

```yaml
skillId: savings

basic:
  mastery: 3

advanced:
  mastery: 1
```

### 15.5. Evidence types

Supported:

```text
knowledgeEvidence
scenarioEvidence
behaviourEvidence
transferEvidence
```

Tests and scenarios may both confirm a result if they actually measure that result.

Behaviour outcomes such as regular saving must not be completed by a single correct quiz answer.

Mastery 3 should normally require:
- repeated application;
- another context;
- or a meaningful transfer situation.

### 15.6. Skill definitions

#### `budget_management`

BASIC:
- distinguish income and expenses;
- distinguish mandatory and optional expenses;
- understand that expenses should not exceed income;
- create a simple personal budget;
- make decisions consistent with that budget.

ADVANCED:
- calculate amounts for spending and savings;
- reduce optional expenses;
- choose necessary over desired when resources are limited.

#### `savings`

BASIC:
- understand why savings exist;
- set a savings goal;
- create a simple plan;
- regularly save part of personal money.

ADVANCED:
- estimate time to goal;
- judge goal realism;
- prefer saving over an impulsive purchase.

#### `purchase_decision`

BASIC:
- plan a purchase;
- identify needed purchases;
- justify item choice;
- check the result of a purchase.

ADVANCED:
- account for approximate cost in advance;
- resist unplanned purchase pressure;
- make a reasoned choice.

#### `price_evaluation`

BASIC:
- understand price-tag information;
- compare prices;
- calculate total purchase cost.

ADVANCED:
- compare similar offers;
- choose using price, quality, and need.

#### `debt_management`

BASIC:
- understand debt;
- understand repayment responsibility;
- understand that debt has risk.

ADVANCED:
- evaluate debt consequences;
- understand when debt helps in an unexpected situation;
- understand when debt worsens the financial position;
- make a considered borrowing decision.

#### `financial_security`

BASIC:
- protect personal financial information;
- do not share passwords/logins/card data;
- do not follow unknown links;
- ask a trusted adult for help in a suspicious situation.

ADVANCED:
- recognize fraud;
- select a safe response;
- resist manipulation.

#### `digital_finance`

BASIC:
- understand the purpose of a budgeting/savings app;
- record income and expenses;
- use the app for planning and saving.

ADVANCED:
- regularly keep records;
- review own decisions using history and period summaries.

### 15.7. Attitudes are not single-click scores

Framework outcomes such as:
- responsibility;
- motivation;
- caution;
- willingness;
- striving;

must not be converted into a numeric score after one click.

Use them as:
- long-term behaviour indicators;
- parent-facing qualitative observations;
- scenario-selection signals.

---

## 16. XP and Pet Progression

XP is a **game progression system**, not educational mastery.

SkillId and XP are independent systems.

Do not implement:

```text
Skill mastery → automatic XP
```

as the only progression relationship.

Primary XP sources may include:
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
- plain-language BASIC / ADVANCED progress;
- qualitative observations.

Do not show judgmental labels.

Preferred:

```text
Budget planning
Basic: applies confidently
Advanced: learning in progress
```

Avoid:

```text
budget_management = 2/3
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
- SkillId BASIC/ADVANCED mastery;
- evidence;
- XP;
- pet stage;
- unlocked mini-games/content.

Schema changes require migrations.

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
Events → Skills → Events
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
- SkillId mastery transitions;
- evidence recording;
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
- evidence recorded;
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

### SkillProgress

```text
skillId
basicMastery
advancedMastery
needsRepeat flags / equivalent
evidence history
```

### XP / Progression

```text
xp
gameLevel
petStage
unlocks
```

Do not mix SkillId mastery and XP in one numeric field.

---

## 26. Design-System Rules

There is no final brandbook assumption unless the repository contains one.

Feature code must not hardcode brand styling.

Avoid:
- one-off colors;
- arbitrary fonts;
- repeated custom radii;
- duplicate button styles;
- one-off status components.

Use semantic tokens such as:

```text
actionPrimary
surfaceBase
surfaceElevated
textPrimary
textSecondary
statusPositive
statusWarning
statusCritical
```

The design system should own:
- colors;
- typography;
- spacing;
- shapes;
- elevation;
- icons;
- touch targets;
- motion;
- common buttons;
- purchase summary;
- plan cards;
- feedback cards;
- progress indicators;
- item cards;
- confirmation sheets.

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
- SkillId BASIC/ADVANCED mastery transitions;
- evidence recording;
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

## 31. Feature Documentation Requirement

Before implementing a non-trivial feature, create or update:

```text
docs/features/<feature_name>.md
```

Each feature document must describe:

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
- SkillId integration;
- configuration;
- edge cases;
- acceptance criteria.

Feature documentation is a living document and must be updated with implementation changes.

---

## 32. Feature Ownership Template

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

## Skill Integration
SkillId:
Framework level:
Evidence types:
Feedback:

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
- maps to one of the canonical SkillIds;
- BASIC/ADVANCED is explicit;
- evidence type is explicit;
- feedback explains consequence;
- poor choices do not delete prior mastery.

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
- feature documentation is current.

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
- merge XP and SkillId into one progression number;
- create dozens of near-duplicate SkillIds;
- score motivation/responsibility from one click;
- turn the notebook into the whole game;
- make mini-games the main income source;
- decrease XP/mastery after mistakes;
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
└── parallel educational evidence
    ↓
SkillId BASIC / ADVANCED
    ↓
Parent section
```

When uncertain, prefer consistency with:
- weekly planning;
- shared economy contracts;
- hidden educational analytics;
- independent XP;
- neutral consequences;
- the child-facing virtual-pet experience.
