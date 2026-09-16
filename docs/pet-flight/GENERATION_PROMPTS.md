# Необязательные промпты
Статус: не запускались. Runtime v1 использует Canvas и существующего питомца. Генерация нужна только при решении заменить художественную геометрию; физика, UI и идентичность питомца неизменны.

## Общий блок
Create one original warm pixel-art game asset for FinPet, a friendly children's virtual-pet game. Clear grouped pixels, calm daylight, restrained detail, consistent light from upper left, readable at small size. No text, typography, UI, logos, currency, characters, pipes, copied game layouts or reference-game assets. Match the supplied current FinPet room only for palette and pixel density. Keep gameplay silhouettes unmistakable. Deliver one asset, not a contact sheet. Export dimensions are preparation targets.

## Фон cloud_trail
A tranquil open sky above distant floating gardens, layered pale clouds and tiny far-away islands, portrait composition. Center area calm and nearly empty, no obstacles or foreground objects. Seamless left-right continuation for slow horizontal scrolling. No pet. Opaque background, target720×1120. Do not bake highlights that look like collectibles.

## Верхний остров cloud_gate_top
One floating garden island extending from the TOP edge of a transparent canvas, with a perfectly horizontal flat lower boundary and softened stepped pixel corners only outside the collision silhouette. Roots and flowers extend upward, away from the clear space below. Orthographic side view, no perspective. Target256×768RGBA. Leave no stray pixels below the flat boundary.

## Нижний остров cloud_gate_bottom
One floating garden island extending from the BOTTOM edge of a transparent canvas, perfectly horizontal flat upper boundary at a measured anchor, small grass details growing away from the gameplay opening. Friendly green terrace, layered earth, orthographic side view. Transparent beyond island, target256×768RGBA. No treasure, coins, spikes or text.

## Питомец
Не создавать нового героя текстовым промптом. Если общий pet renderer получит позы полёта: редактировать проверенный текущий ресурс, сохраняя силуэт вида, оттенки, глаза, аксессуары и масштаб. Получить нейтральную и радостную позы на общем холсте, проверить вместе с комнатой. Ссылки на Flappy Bird не использовать как изображение-референс.
