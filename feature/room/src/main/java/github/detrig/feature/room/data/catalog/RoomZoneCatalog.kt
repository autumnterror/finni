package github.detrig.feature.room.data.catalog

import github.detrig.feature.room.domain.model.RoomZoneDefinition
import github.detrig.feature.gamestate.domain.model.MiniGameAccess

/** Экономический каталог. Координаты предметов принадлежат конфигурации HouseLayout. */
internal class RoomZoneCatalog {
    val zones: List<RoomZoneDefinition> = listOf(
        RoomZoneDefinition("ball", "ball", "ball", 0, 1,
            initiallyOpen = MiniGameAccess.isInitiallyOpen("ball")),
        RoomZoneDefinition("drawing", "drawing", "drawing", 200, 1),
        RoomZoneDefinition("music", "music", "music", 350, 1),
        RoomZoneDefinition("workshop", "workshop", "workshop", 450, 2),
        RoomZoneDefinition("puzzle", "puzzle", "puzzle", 250, 1),
        RoomZoneDefinition("fishing", "fishing", "fishing", 400, 1),
        RoomZoneDefinition("garden", "garden", "garden", 550, 2),
        RoomZoneDefinition("flight", "flight", "flight", 300, 1,
            initiallyOpen = MiniGameAccess.isInitiallyOpen("flight")),
        RoomZoneDefinition("football", "football", "football", 650, 3),
        RoomZoneDefinition("space", "space", "space", 700, 3),
        RoomZoneDefinition("racing", "racing", "racing", 800, 4),
        RoomZoneDefinition("theater", "theater", "theater", 900, 4),
        RoomZoneDefinition("science", "science", "science", 1_000, 5),
    )
}
