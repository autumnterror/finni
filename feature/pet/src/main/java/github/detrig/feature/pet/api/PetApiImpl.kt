package github.detrig.feature.pet.api

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.model.GrowthStage
import github.detrig.feature.pet.domain.repository.PetRepository
import github.detrig.feature.pet.presentation.HamsterAssetsCache
import github.detrig.feature.pet.presentation.PetHostScreen
import github.detrig.feature.pet.presentation.PetScene
import github.detrig.feature.pet.presentation.PetPortrait
import github.detrig.feature.pet.presentation.ClothingArtwork
import github.detrig.feature.pet.presentation.ClothingThumbnail as PetClothingThumbnail
import github.detrig.feature.pet.presentation.HamsterPreview
import github.detrig.feature.pet.presentation.rememberHamsterAssets
import github.detrig.feature.pet.presentation.rememberClothingLayers
import github.detrig.feature.pet.presentation.rememberPetAppearanceBitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PetApiImpl(
    private val repository: PetRepository,
    private val assets: android.content.res.AssetManager,
    private val growthStages: Flow<GrowthStage>,
    private val preferences: SharedPreferences,
    private val allowDebugGrowthOverride: Boolean,
) : PetApi {
    private val debugOverride = MutableStateFlow(
        if (allowDebugGrowthOverride) {
            GrowthStage.fromAssetId(preferences.getString(DEBUG_GROWTH_STAGE_KEY, null))
        } else null,
    )
    private val overrideMutex = Mutex()
    @Volatile
    private var growthSnapshot = PetGrowthState(GrowthStage.BABY, GrowthStage.BABY, false)
    private val growthStateFlow = combine(growthStages, debugOverride) { fromLevel, override ->
        PetGrowthState(
            stage = override ?: fromLevel,
            stageFromLevel = fromLevel,
            isDebugOverride = override != null,
        )
    }.distinctUntilChanged().onEach { growthSnapshot = it }

    @Composable
    private fun growthStage(): GrowthStage {
        val state by growthStateFlow.collectAsState(initial = growthSnapshot)
        return state.stage
    }

    override fun observeGrowthState(): Flow<PetGrowthState> = growthStateFlow

    override suspend fun adjustGrowthStageForDebug(delta: Int): PetGrowthState = overrideMutex.withLock {
        check(allowDebugGrowthOverride)
        require(delta == -1 || delta == 1)
        val current = growthStateFlow.first()
        val next = GrowthStage.entries[
            (current.stage.ordinal + delta).coerceIn(0, GrowthStage.entries.lastIndex)
        ]
        check(preferences.edit().putString(DEBUG_GROWTH_STAGE_KEY, next.assetId).commit())
        debugOverride.value = next
        current.copy(stage = next, isDebugOverride = true)
    }

    override suspend fun useLevelGrowthStageForDebug(): PetGrowthState = overrideMutex.withLock {
        check(allowDebugGrowthOverride)
        check(preferences.edit().remove(DEBUG_GROWTH_STAGE_KEY).commit())
        debugOverride.value = null
        growthStateFlow.first()
    }

    override suspend fun preloadAssets() {
        growthStateFlow.first()
        HamsterAssetsCache.awaitPreloaded(assets)
        ClothingArtwork.preload(assets, repository.currentProfile())
    }

    override fun observeProfile() = repository.observeProfile()

    override fun currentProfile(): PetProfile? = repository.currentProfile()

    override fun resetProfile() {
        repository.resetProfile()
        debugOverride.value = null
        growthSnapshot = PetGrowthState(GrowthStage.BABY, GrowthStage.BABY, false)
        ClothingArtwork.clearEquipped()
    }

    override suspend fun clothingItems(): List<ClothingItem> = ClothingArtwork.items(assets)

    override fun cachedClothingItems(): List<ClothingItem> = ClothingArtwork.cachedItems()

    override fun recordClothingPurchase(itemId: String) {
        repository.updateClothing { it.withPurchase(itemId) }
    }

    override fun equipClothing(slot: String, itemId: String?) {
        val profile = repository.updateClothing { it.withEquipped(slot, itemId) }
        ClothingArtwork.retainEquipped(profile)
    }

    @Composable
    override fun ClothingThumbnail(itemId: String, modifier: Modifier) {
        PetClothingThumbnail(itemId, modifier)
    }

    @Composable
    override fun OutfitPreview(profile: PetProfile, equippedBySlot: Map<String, String>, modifier: Modifier) {
        val hamsterAssets = rememberHamsterAssets()
        if (hamsterAssets != null) {
            HamsterPreview(
                assets = hamsterAssets,
                appearance = profile.hamsterAppearance,
                stage = growthStage(),
                modifier = modifier,
                clothingLayers = rememberClothingLayers(equippedBySlot, profile.hamsterAppearance),
            )
        }
    }

    @Composable
    override fun RequirePet(
        modifier: Modifier,
        content: @Composable (PetProfile, () -> Unit, () -> Unit, Boolean) -> Unit,
    ) {
        PetHostScreen(modifier = modifier, growthStage = growthStage(), content = content)
    }

    @Composable
    override fun Content(
        profile: PetProfile,
        modifier: Modifier,
        onClick: (() -> Unit)?,
        animateIdle: Boolean,
        freezeAnimation: Boolean,
        mouthOpen: Boolean,
        lookAt: Offset?,
        pose: PetPose,
        gestureCallbacks: PetGestureCallbacks?,
        showShadow: Boolean,
        dirtStage: Int,
    ) {
        PetScene(
            profile = profile,
            modifier = modifier,
            growthStage = growthStage(),
            animateIdle = animateIdle,
            freezeAnimation = freezeAnimation,
            mouthOpen = mouthOpen,
            lookAt = lookAt,
            onClick = onClick,
            pose = pose,
            gestureCallbacks = gestureCallbacks,
            showShadow = showShadow,
            dirtStage = dirtStage,
        )
    }

    @Composable
    override fun Portrait(profile: PetProfile, modifier: Modifier, dirtStage: Int) {
        PetPortrait(profile, modifier, growthStage = growthStage(), dirtStage = dirtStage)
    }

    @Composable
    override fun rememberCurrentAppearanceBitmap(maxSidePx: Int): ImageBitmap? {
        val profile by repository.observeProfile().collectAsState(initial = null)
        val stage = growthStage()
        return profile?.let { rememberPetAppearanceBitmap(it, maxSidePx, stage) }
    }

    private companion object {
        const val DEBUG_GROWTH_STAGE_KEY = "debug_growth_stage_v1"
    }
}
