package github.detrig.feature.room.domain.surface

import android.content.res.Resources
import github.detrig.feature.room.R
import org.json.JSONObject

internal enum class SurfaceKind(val id: String, val label: String) {
    WALL("wall", "Обои"),
    FLOOR("floor", "Пол"),
    ;

    companion object {
        fun fromId(id: String): SurfaceKind = entries.first { it.id == id }
    }
}

internal data class SurfaceVariant(
    val id: String,
    val roomId: String,
    val kind: SurfaceKind,
    val name: String,
    val priceRub: Long,
    val drawableId: Int,
) {
    val slotId: String get() = surfaceSlotId(roomId, kind)
}

internal fun surfaceSlotId(roomId: String, kind: SurfaceKind): String = "$roomId:${kind.id}"

internal class SurfaceCatalog(resources: Resources) {
    val variants: List<SurfaceVariant>
    val byId: Map<String, SurfaceVariant>
    val bySlot: Map<String, List<SurfaceVariant>>

    init {
        val raw = resources.assets.open("surface_catalog.json").bufferedReader().use { it.readText() }
        val entries = JSONObject(raw).getJSONArray("surfaces")
        val resourcePackage = resources.getResourcePackageName(R.drawable.room_bed)
        variants = List(entries.length()) { index ->
            val entry = entries.getJSONObject(index)
            val drawableName = entry.getString("drawable")
            val drawableId = resources.getIdentifier(drawableName, "drawable", resourcePackage)
            require(drawableId != 0) { "Missing surface drawable $drawableName" }
            SurfaceVariant(
                id = entry.getString("id"),
                roomId = entry.getString("room").also { require(it in ROOM_IDS) },
                kind = SurfaceKind.fromId(entry.getString("kind")),
                name = entry.getString("name"),
                priceRub = entry.getLong("price"),
                drawableId = drawableId,
            )
        }
        byId = variants.associateBy(SurfaceVariant::id)
        bySlot = variants.groupBy(SurfaceVariant::slotId)
        require(byId.size == variants.size)
        require(ROOM_IDS.all { roomId ->
            SurfaceKind.entries.all { kind -> bySlot[surfaceSlotId(roomId, kind)]?.size == 25 }
        })
    }

    private companion object {
        val ROOM_IDS = setOf("playroom", "bedroom", "living", "kitchen")
    }
}
