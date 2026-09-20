package github.detrig.internetbooster.startup

import github.detrig.feature.pet.PetFeature
import github.detrig.feature.room.RoomFeature

/** Loads process-wide visual assets before the navigation graph is shown. */
internal class AppAssetPreloader {
    suspend fun preload(onProgress: (Float) -> Unit) {
        val roomApi = RoomFeature.getApi()
        val petApi = PetFeature.getApi()

        onProgress(0.08f)
        roomApi.preloadAssets()
        onProgress(0.55f)
        petApi.preloadAssets()
        onProgress(1f)
    }
}
