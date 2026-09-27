package github.detrig.feature.room.domain.furniture

import android.content.res.Resources
import org.json.JSONObject
import github.detrig.feature.room.R

internal data class FurnitureSlot(
    val id: String,
    val placementId: String,
    val roomId: String,
    val label: String,
)

internal data class FurnitureVariant(
    val id: String,
    val slotId: String,
    val placementId: String,
    val name: String,
    val priceRub: Long,
    val drawableId: Int,
    val supportDelta: Float,
)

internal class FurnitureCatalog(resources: Resources) {
    val slots: List<FurnitureSlot>
    val variants: List<FurnitureVariant>
    val byId: Map<String, FurnitureVariant>
    val bySlot: Map<String, List<FurnitureVariant>>
    val slotByPlacement: Map<String, FurnitureSlot>

    init {
        val raw = resources.assets.open("interior_catalog.json").bufferedReader().use { it.readText() }
        val catalog = JSONObject(raw)
        val slotArray = catalog.getJSONArray("slots")
        slots = List(slotArray.length()) { index ->
            val slot = slotArray.getJSONObject(index)
            FurnitureSlot(
                id = slot.getString("id"),
                placementId = slot.getString("placement"),
                roomId = slot.getString("room"),
                label = slot.getString("label"),
            )
        }
        val variantArray = catalog.getJSONArray("variants")
        val placementsBySlot = slots.associate { it.id to it.placementId }
        val resourcePackage = resources.getResourcePackageName(R.drawable.room_bed)
        variants = List(variantArray.length()) { index ->
            val variant = variantArray.getJSONObject(index)
            val drawableName = variant.getString("drawable")
            val drawableId = resources.getIdentifier(drawableName, "drawable", resourcePackage)
            require(drawableId != 0) { "Missing interior drawable $drawableName" }
            FurnitureVariant(
                id = variant.getString("id"),
                slotId = variant.getString("slot"),
                placementId = requireNotNull(placementsBySlot[variant.getString("slot")]),
                name = variant.getString("name"),
                priceRub = variant.getLong("price"),
                drawableId = drawableId,
                supportDelta = variant.getDouble("supportDelta").toFloat(),
            )
        }
        byId = variants.associateBy { it.id }
        bySlot = variants.groupBy { it.slotId }
        slotByPlacement = slots.associateBy { it.placementId }
        require(slots.isNotEmpty() && variants.isNotEmpty())
        require(slots.all { it.roomId in setOf("bedroom", "living", "kitchen") && bySlot[it.id].orEmpty().isNotEmpty() })
        require(byId.size == variants.size && slotByPlacement.size == slots.size)
    }
}
