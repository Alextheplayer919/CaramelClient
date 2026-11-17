package org.chimeramc.client.core.cosmetics;

import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometry;
import org.chimeramc.client.core.cosmetics.geometry.BedrockGeometryParser;
import org.chimeramc.client.core.cosmetics.geometry.PreviewMeshModel;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class PetProbeTest {
    private static File root() {
        File d = new File("").getAbsoluteFile();
        for (int i = 0; i < 6 && d != null; i++) {
            if (new File(d, "app").isDirectory() && new File(d, "resources").isDirectory()) return d;
            d = d.getParentFile();
        }
        return new File("").getAbsoluteFile();
    }
    @Test
    public void probe() throws Exception {
        File dir = new File(root(), "resources/cosmetics/models/pets");
        for (String id : AuthoredPetModels.IDS) {
            BedrockGeometry geo = BedrockGeometryParser.parse(new String(Files.readAllBytes(new File(dir, id + ".geo.json").toPath())));
            PetAssetCoverageTest.PngImage img = PetAssetCoverageTest.PngImage.decode(Files.readAllBytes(new File(dir, id + ".png").toPath()));
            List<PreviewMeshModel.Box> mesh = PreviewMeshModel.build(geo);
            PreviewMeshModel.normalizeToHeight(mesh, 14f);
            StringBuilder sb = new StringBuilder(id + " ");
            for (int yaw = 0; yaw < 360; yaw += 45) {
                PreviewScene scene = new PreviewScene();
                PetPose pose = new PetPose();
                pose.compute(CosmeticCatalog.PetSpecies.CAT, CosmeticCatalog.PetLocomotion.WALK, 0f);
                PreviewAuthoredScene.addPet(scene, mesh, img.argb(), img.width(), img.height(),
                        geo.models.get(0).description.textureWidth, geo.models.get(0).description.textureHeight, 0f, 0f, 0f, pose);
                PreviewRaster r = new PreviewRaster();
                int W = 160;
                r.prepare(W, W, 0x00000000);
                SkinModel.Camera cam = PreviewScene.cameraFor(yaw);
                PreviewScene.Fit fit = scene.fit(cam, W, W, 0.06f);
                scene.render(r, cam, fit);
                int n = 0; for (int p : r.pixels()) if ((p >>> 24) != 0) n++;
                sb.append(n).append(" ");
            }
            System.out.println(sb);
        }
    }
}
