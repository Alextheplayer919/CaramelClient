package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertTrue;

import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometry;
import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometryParser;
import org.chimeramc.client.core.cosmetics.geometry.PreviewMeshModel;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * A hat preview must be solid from every angle, not see-through.
 *
 * <p>Many authored hats are open shells: a cap, a halo or a headband has no back face authored,
 * because in Blockbench the inside is never seen. The renderer culled back faces by the vertex
 * normal, so at the angles where only the inner surface faced the camera the hat vanished and the
 * head showed straight through it — the "hat previews are transparent from different angles"
 * report. {@link PreviewAuthoredScene} now draws an accessory double-sided (a pet, being a closed
 * solid, stays culled).
 *
 * <p>This reads every shipped {@code .geo.json}/{@code .png} pair, builds the mesh, and asserts both
 * halves of the fix: the accessory faces are double-sided, and the rendered silhouette does not
 * collapse at any of the eight yaw angles the preview turns through (a collapsed silhouette is what
 * a see-through hat looks like).
 */
public class HatPreviewCoverageTest {

    private static final int W = 200;
    private static final float MIN_SILHOUETTE_RATIO = 0.2f;

    @Test
    public void anAuthoredAccessoryIsDrawnFromBothSides() {
        PreviewScene scene = new PreviewScene();
        PreviewAuthoredScene.addAccessory(scene, mesh("vb_chef_hat"), atlas("vb_chef_hat"),
                atlasWidth, atlasHeight, geoTextureWidth("vb_chef_hat"), geoTextureHeight("vb_chef_hat"),
                0f, 0f, true);
        assertTrue("the authored hat produced no faces", !scene.faces().isEmpty());
        for (PreviewScene.Face f : scene.faces()) {
            assertTrue("a hat face must be double-sided or it vanishes at the angles where only its "
                    + "inner surface faces the camera", f.doubleSided);
        }
    }

    @Test
    public void everyAuthoredHatStaysSolidAtEveryAngle() throws Exception {
        List<String> offenders = new ArrayList<>();
        int checked = 0;
        for (String id : AuthoredHatModels.IDS) {
            if (!hasAssets(id)) continue;
            checked++;
            List<PreviewMeshModel.Box> mesh = mesh(id);
            int[] atlas = atlas(id);
            int gw = geoTextureWidth(id);
            int gh = geoTextureHeight(id);
            int min = Integer.MAX_VALUE;
            int max = 0;
            for (int yaw = 0; yaw < 360; yaw += 45) {
                int painted = paintedAt(mesh, atlas, gw, gh, yaw);
                min = Math.min(min, painted);
                max = Math.max(max, painted);
            }
            if (max == 0) {
                offenders.add(id + " painted nothing");
                continue;
            }
            float ratio = min / (float) max;
            if (ratio < MIN_SILHOUETTE_RATIO) {
                offenders.add(id + " collapses at some angle (min/max=" + ratio + ")");
            }
        }
        assertTrue("expected the shipped authored hats to be present", checked > 0);
        assertTrue("an authored hat preview is see-through from some angles: " + offenders,
                offenders.isEmpty());
    }

    private static int paintedAt(List<PreviewMeshModel.Box> mesh, int[] atlas,
                                 int gw, int gh, int yaw) {
        PreviewScene scene = new PreviewScene();
        PreviewAuthoredScene.addAccessory(scene, mesh, atlas, atlasWidth, atlasHeight, gw, gh,
                0f, 0f, true);
        PreviewRaster raster = new PreviewRaster();
        raster.prepare(W, W, 0x00000000);
        SkinModel.Camera camera = PreviewScene.cameraFor(yaw);
        PreviewScene.Fit fit = scene.fit(camera, W, W, 0.06f);
        scene.render(raster, camera, fit);
        int painted = 0;
        for (int p : raster.pixels()) if ((p >>> 24) != 0) painted++;
        return painted;
    }

    private static boolean hasAssets(String id) {
        return new File(dir(), id + ".geo.json").isFile() && new File(dir(), id + ".png").isFile();
    }

    private static List<PreviewMeshModel.Box> mesh(String id) {
        return PreviewMeshModel.build(geometry(id));
    }

    private static BedrockGeometry geometry(String id) {
        try {
            return BedrockGeometryParser.parse(new String(
                    Files.readAllBytes(new File(dir(), id + ".geo.json").toPath()),
                    StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("could not parse " + id, e);
        }
    }

    private static int geoTextureWidth(String id) {
        return geometry(id).models.get(0).description.textureWidth;
    }

    private static int geoTextureHeight(String id) {
        return geometry(id).models.get(0).description.textureHeight;
    }

    private static int atlasWidth;
    private static int atlasHeight;

    private static int[] atlas(String id) {
        try {
            PetAssetCoverageTest.PngImage image = PetAssetCoverageTest.PngImage.decode(
                    Files.readAllBytes(new File(dir(), id + ".png").toPath()));
            if (image == null) return null;
            atlasWidth = image.width();
            atlasHeight = image.height();
            return image.argb();
        } catch (Exception e) {
            return null;
        }
    }

    private static File dir() {
        return new File(repoRoot(), "resources/cosmetics/models/hats");
    }

    private static File repoRoot() {
        File dir = new File("").getAbsoluteFile();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (new File(dir, "app").isDirectory() && new File(dir, "resources").isDirectory()) {
                return dir;
            }
            dir = dir.getParentFile();
        }
        return new File("").getAbsoluteFile();
    }
}
