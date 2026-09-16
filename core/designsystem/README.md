# FinPet Design System

`core:designsystem` is the only source of reusable visual decisions. Feature code
uses semantic tokens through `AppTheme` and shared components from the
`component` package.

## Token layers

- `FinPetThemePack` is the complete replaceable visual contract.
- `FinPetThemePacks.prototype` contains temporary values used before the
  brandbook is ready.
- `AppTheme` exposes colors, typography, spacing, shapes, sizes, elevation, and
  motion to Compose code.
- Shared components own the Material 3 implementation details.

Use semantic names such as `actionPrimary`, `currencyAccent`, `screenTitle`, or
`metricValue`. Do not expose palette names such as `Green500` or `Purple40`.

Keep one-off layout measurements close to their component. Promote a value into
the theme only when it is a repeated visual decision or part of an accessibility
contract.

The continuous house uses `AppTheme.colors.house` (`FinPetHouseColors`) for wall,
floor, furniture, toy, outline, and fog colors. Its vector artwork consumes this
palette; room controls reuse shared spacing, sizes, typography, and motion.
Existing `roomBackground` and `roomObject*` tokens remain available to shared UI.

## Applying the brandbook

1. Add the brand fonts and reusable brand assets to this module.
2. Create a brand theme pack with the same `FinPetThemePack` contract.
3. Update shared components when the brandbook changes component behavior, not
   only token values.
4. Change the default pack in `FinPetTheme`.
5. Run `gradlew checkDesignSystemUsage`, design-system tests, and reference-screen
   visual checks.

No feature module should need palette, typography, shape, or standard control
changes during this migration.
