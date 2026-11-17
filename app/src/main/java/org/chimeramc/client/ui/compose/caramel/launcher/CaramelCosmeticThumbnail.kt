package org.chimeramc.client.ui.compose.caramel.launcher

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import org.chimeramc.client.core.cosmetics.AuthoredCapeTextures
import org.chimeramc.client.core.cosmetics.AuthoredHatModels
import org.chimeramc.client.core.cosmetics.AuthoredPetModels
import org.chimeramc.client.core.cosmetics.CapeAtlas
import org.chimeramc.client.core.cosmetics.CapeThumbnail
import org.chimeramc.client.core.cosmetics.CosmeticCatalog
import org.chimeramc.client.core.cosmetics.CosmeticThumbnail
import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometryParser
import org.chimeramc.client.core.cosmetics.geometry.PreviewMeshModel
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticInfo
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticKind

/**
 * The real art for a Cosmetics tile, rendered by the shipped software renderers.
 *
 * <p>The grid drew a generic coloured rectangle per tile — the same block for a cape, a hat and a
 * pet — even though the client ships the renderers that draw each one for real ([CapeThumbnail] for
 * a cape's cloth, [CosmeticThumbnail] for a hat, a back piece or a pet). Nothing called them, so the
 * list never showed the cosmetic a player was about to equip. This renders each tile from its
 * actual catalogue entry and, when it has one, its actual authored texture and mesh.
 *
 * <p>Fail-closed: a cosmetic whose asset is missing paints nothing rather than a broken frame, so the
 * tile's gradient shows through instead of the whole grid failing.
 */
@Composable
fun CaramelCosmeticThumbnail(info: CaramelCosmeticInfo, modifier: Modifier, sizePx: Int) {
    val context = LocalContext.current
    val bitmap = remember(info.id, info.kind, sizePx) {
        CosmeticTileRenderer.render(context, info.id, info.kind, sizePx)
    }
    // A tile scrolled off the grid must free its bitmap, or the grid leaks one per cosmetic seen.
    DisposableEffect(bitmap) {
        onDispose { bitmap?.recycle() }
    }
    if (bitmap == null) return
    Box(modifier, Alignment.Center) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

/** Renders a cosmetic's own art into a square ARGB bitmap. */
internal object CosmeticTileRenderer {

    fun render(context: Context, id: String, kind: CaramelCosmeticKind, size: Int): Bitmap? {
        if (context == null || size <= 0) return null
        val out = IntArray(size * size)
        val ok = when (kind) {
            CaramelCosmeticKind.Cape -> renderCape(context, id, size, out)
            CaramelCosmeticKind.Hat, CaramelCosmeticKind.Wings ->
                renderAccessory(context, id, size, out)
            CaramelCosmeticKind.Pet -> renderPet(context, id, size, out)
        }
        return if (ok) toBitmap(out, size) else null
    }

    private fun renderCape(context: Context, id: String, size: Int, out: IntArray): Boolean {
        val cape = CosmeticCatalog.capes().firstOrNull { it.id == id } ?: return false
        if (!AuthoredCapeTextures.hasAuthoredTexture(cape.id)) {
            CapeThumbnail.renderFitted(cape, size, size, out, 0)
            return true
        }
        val atlas = decodeAsset(context,
            AuthoredCapeTextures.DIR + "/" + AuthoredCapeTextures.fileFor(cape.id)) ?: return false
        val aw = atlas.width
        val ah = atlas.height
        val pixels = IntArray(aw * ah)
        atlas.getPixels(pixels, 0, aw, 0, 0, aw, ah)
        atlas.recycle()
        // Letterbox the 10:16 panel at its true aspect ratio, so the whole cape shows undistorted.
        var clothW = size
        var clothH = Math.round(size / CapeThumbnail.ASPECT)
        if (clothH > size) {
            clothH = size
            clothW = Math.round(size * CapeThumbnail.ASPECT)
        }
        val x0 = (size - clothW) / 2
        val y0 = (size - clothH) / 2
        for (y in 0 until clothH) {
            val v = (y + 0.5f) / clothH
            val verticalShade = 1.0f - 0.16f * v
            for (x in 0 until clothW) {
                val u = (x + 0.5f) / clothW
                val colour = CapeAtlas.sample(pixels, aw, ah, u, v)
                val sideShade = 0.94f + 0.06f * (1f - Math.abs(u - 0.5f) * 2f)
                out[(y0 + y) * size + x0 + x] = scaleArgb(colour, verticalShade * sideShade)
            }
        }
        return true
    }

    private fun renderAccessory(context: Context, id: String, size: Int, out: IntArray): Boolean {
        val accessory = CosmeticCatalog.accessories().firstOrNull { it.id == id } ?: return false
        val mesh = loadMesh(context, accessory.modelId,
            { modelId -> AuthoredHatModels.loadFromAssets(assetOpener(context), modelId) },
            AuthoredHatModels::hasAuthoredModel)
        val atlas = if (mesh != null) {
            decodeAsset(context,
                AuthoredHatModels.DIR + "/" + AuthoredHatModels.textureFor(accessory.modelId))
        } else null
        val pixels = atlas?.let { a -> IntArray(a.width * a.height).also {
            a.getPixels(it, 0, a.width, 0, 0, a.width, a.height) } }
        CosmeticThumbnail.renderAccessory(accessory, mesh, pixels,
            atlas?.width ?: 0, atlas?.height ?: 0, 64, 64, size, size, out, 0)
        atlas?.recycle()
        return true
    }

    private fun renderPet(context: Context, id: String, size: Int, out: IntArray): Boolean {
        val pet = CosmeticCatalog.pets().firstOrNull { it.id == id } ?: return false
        val petId = pet.modelId ?: pet.id
        val mesh = loadMesh(context, petId,
            { modelId -> AuthoredPetModels.loadFromAssets(assetOpener(context), modelId) },
            AuthoredPetModels::hasAuthoredModel)
        val atlas = if (mesh != null) {
            decodeAsset(context, AuthoredPetModels.DIR + "/" + AuthoredPetModels.textureFor(petId))
        } else null
        val pixels = atlas?.let { a -> IntArray(a.width * a.height).also {
            a.getPixels(it, 0, a.width, 0, 0, a.width, a.height) } }
        CosmeticThumbnail.renderPet(pet, mesh, pixels,
            atlas?.width ?: 0, atlas?.height ?: 0, 64, 64, size, size, out, 0)
        atlas?.recycle()
        return true
    }

    /** The authored mesh for a model id, or null when it has none or cannot be read. */
    private fun loadMesh(context: Context, modelId: String?,
                         loadJson: (String) -> String?,
                         hasModel: (String) -> Boolean): List<PreviewMeshModel.Box>? {
        if (modelId == null || !hasModel(modelId)) return null
        val json = loadJson(modelId) ?: return null
        val geometry = BedrockGeometryParser.parse(json) ?: return null
        return PreviewMeshModel.build(geometry).ifEmpty { null }
    }

    private fun assetOpener(context: Context) =
        org.chimeramc.client.core.cosmetics.AuthoredGeometry.AssetOpener { path ->
            context.assets.open(path)
        }

    private fun decodeAsset(context: Context, path: String): Bitmap? = try {
        context.assets.open(path).use { android.graphics.BitmapFactory.decodeStream(it) }
    } catch (t: Throwable) {
        null
    }

    private fun toBitmap(pixels: IntArray, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    }

    private fun scaleArgb(color: Int, factor: Float): Int {
        val a = (color ushr 24) and 0xFF
        val r = clamp((((color ushr 16) and 0xFF) * factor).toInt())
        val g = clamp((((color ushr 8) and 0xFF) * factor).toInt())
        val b = clamp(((color and 0xFF) * factor).toInt())
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun clamp(value: Int): Int = if (value < 0) 0 else if (value > 255) 255 else value
}
