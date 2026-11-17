package org.chimeramc.client.core.cosmetics;

/**
 * Builds the cape cloth mesh into the software scene from a {@link CapeSimulator}.
 *
 * <p>The cloth is the same Verlet mesh the Canvas pass drew: a {@code COLS x ROWS} grid of particles
 * in blocks, hung from the shoulders. Each grid cell becomes a flat-shaded quad; the colour comes from
 * {@link CapePatterns} at the cell's centre (so the menu preview and the worn cape show the same
 * weave), the shade from the quad's own model-space normal (so a fold curling toward the light
 * brightens), and the depth buffer resolves it against the body per pixel — no manual occlusion test.
 *
 * <p>Pure and Android-free, so it is unit-testable.
 */
public final class PreviewCapeScene {

    /** Where the cloth hangs from, in model pixels/blocks, matching the Canvas pass. */
    public static final float ANCHOR_Y = 24f;
    public static final float ANCHOR_Z = CosmeticLayering.CAPE_Z;

    private PreviewCapeScene() {
    }

    /**
     * Adds the cloth quads to {@code scene}.
     *
     * @param scene  the scene to add to (not cleared)
     * @param cape   the equipped cape (pattern + colours)
     * @param cloth  the stepped Verlet mesh
     */
    public static void addCape(PreviewScene scene, CosmeticCatalog.Cape cape, CapeSimulator cloth) {
        addCape(scene, cape, cloth, null, 0, 0);
    }

    /**
     * Adds the cloth quads to {@code scene}, preferring the cape's authored artwork when it has any.
     *
     * <p>When {@code atlas} is a real authored 64x32 sheet, each cell's colour is sampled from its
     * visible panel ({@link CapeAtlas}) instead of the procedural weave, so the preview shows the
     * artwork the player picked rather than a template. A null or undersized atlas falls back to the
     * weave, so an id with no authored image still draws.
     *
     * @param scene  the scene to add to (not cleared)
     * @param cape   the equipped cape (pattern + colours)
     * @param cloth  the stepped Verlet mesh
     * @param atlas  the authored 64x32 ARGB sheet, or null to use the procedural weave
     * @param atlasW the atlas width in pixels
     * @param atlasH the atlas height in pixels
     */
    public static void addCape(PreviewScene scene, CosmeticCatalog.Cape cape, CapeSimulator cloth,
                               int[] atlas, int atlasW, int atlasH) {
        if (scene == null || cape == null || cloth == null) return;
        boolean authored = atlas != null && atlas.length >= atlasW * atlasH
                && atlasW > 0 && atlasH > 0;
        int cols = CapeSimulator.COLS;
        int rows = CapeSimulator.ROWS;
        if (cloth.size() < cols * rows) return;

        // Model-space corner for a particle: the simulator works in blocks; lift to the shoulders.
        float[][] p = new float[cols * rows][3];
        for (int i = 0; i < cols * rows; i++) {
            p[i][0] = cloth.x(i) / SkinModel.PIXELS_TO_BLOCKS;
            p[i][1] = ANCHOR_Y + cloth.y(i) / SkinModel.PIXELS_TO_BLOCKS;
            p[i][2] = ANCHOR_Z + cloth.z(i) / SkinModel.PIXELS_TO_BLOCKS;
        }

        // Per-cell colours and normals, computed once so the front sheet, the back sheet and the
        // rim can all agree on the shade at a given point.
        int[][] cellColor = new int[rows - 1][cols - 1];
        float[][][] cellNormal = new float[rows - 1][cols - 1][];
        for (int r = 0; r + 1 < rows; r++) {
            for (int c = 0; c + 1 < cols; c++) {
                float[] e1 = sub(p[r * cols + c + 1], p[r * cols + c]);
                float[] e2 = sub(p[(r + 1) * cols + c], p[r * cols + c]);
                float[] n = cross(e1, e2);
                float len = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
                if (len > 1e-6f) { n[0] /= len; n[1] /= len; n[2] /= len; }
                cellNormal[r][c] = n;

                float u = (c + 0.5f) / Math.max(1f, cols - 1f);
                float wv = (r + 0.5f) / Math.max(1f, rows - 1f);
                int clothColor;
                if (authored) {
                    clothColor = CapeAtlas.sample(atlas, atlasW, atlasH, u, wv);
                    if ((clothColor >>> 24) == 0) clothColor = weaveColor(cape, u, wv);
                } else {
                    clothColor = weaveColor(cape, u, wv);
                }
                cellColor[r][c] = clothColor;
            }
        }

        // A cape is a 16x10x1 slab, not a plane: give the sheet a real thickness so its edge shows a
        // rim from the side instead of collapsing to a hairline. Each vertex is pushed back along the
        // average of the cell normals around it, so the back face follows the same fold as the front.
        float thickness = CapeSimulator.THICKNESS_BLOCKS / SkinModel.PIXELS_TO_BLOCKS;
        float[][] back = new float[cols * rows][3];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float[] n = vertexNormal(cellNormal, rows, cols, r, c);
                int i = r * cols + c;
                back[i][0] = p[i][0] - n[0] * thickness;
                back[i][1] = p[i][1] - n[1] * thickness;
                back[i][2] = p[i][2] - n[2] * thickness;
            }
        }

        // Front and back sheets.
        for (int r = 0; r + 1 < rows; r++) {
            for (int c = 0; c + 1 < cols; c++) {
                int i00 = r * cols + c, i10 = r * cols + c + 1;
                int i01 = (r + 1) * cols + c, i11 = (r + 1) * cols + c + 1;
                float[] n = cellNormal[r][c];
                int shaded = PreviewLighting.shadeColorForNormal(cellColor[r][c], n[0], n[1], n[2]);
                addQuad(scene, p[i00], p[i10], p[i01], p[i11], shaded, n, false);
                // The back sheet faces the other way, so it is shaded from the reversed normal.
                int backShaded = PreviewLighting.shadeColorForNormal(
                        cellColor[r][c], -n[0], -n[1], -n[2]);
                addQuad(scene, back[i00], back[i01], back[i10], back[i11], backShaded,
                        new float[]{-n[0], -n[1], -n[2]}, false);
            }
        }

        // The rim: the four perimeter bands joining the front sheet to the back sheet, so the slab's
        // edge is a visible 1-pixel thickness rather than a zero-width seam.
        int rimColor = weaveColor(cape, 0.5f, 0.5f);
        for (int c = 0; c + 1 < cols; c++) {
            addQuad(scene, p[c], p[c + 1], back[c + 1], back[c],
                    PreviewLighting.shadeColorForNormal(rimColor, 0f, 1f, 0f),
                    new float[]{0f, 1f, 0f}, true);
            int top = (rows - 1) * cols + c;
            addQuad(scene, back[top], back[top + 1], p[top + 1], p[top],
                    PreviewLighting.shadeColorForNormal(rimColor, 0f, -1f, 0f),
                    new float[]{0f, -1f, 0f}, true);
        }
        for (int r = 0; r + 1 < rows; r++) {
            int l0 = r * cols, l1 = (r + 1) * cols;
            addQuad(scene, back[l0], back[l1], p[l1], p[l0],
                    PreviewLighting.shadeColorForNormal(rimColor, -1f, 0f, 0f),
                    new float[]{-1f, 0f, 0f}, true);
            int r0 = r * cols + cols - 1, r1 = (r + 1) * cols + cols - 1;
            addQuad(scene, p[r0], p[r1], back[r1], back[r0],
                    PreviewLighting.shadeColorForNormal(rimColor, 1f, 0f, 0f),
                    new float[]{1f, 0f, 0f}, true);
        }
    }

    /** Adds one double-sided-or-not quad with an explicit normal, or nothing if degenerate. */
    private static void addQuad(PreviewScene scene, float[] a, float[] b, float[] c, float[] d,
                                int color, float[] normal, boolean doubleSided) {
        float[][] corners = {a, b, c, d};
        PreviewScene.Face f = scene.addFlatQuad(corners, color);
        if (f == null) return;
        f.nx = normal[0]; f.ny = normal[1]; f.nz = normal[2];
        f.doubleSided = doubleSided;
    }

    /** The average of the (up to four) cell normals touching a grid vertex. */
    private static float[] vertexNormal(float[][][] cellNormal, int rows, int cols, int r, int c) {
        float x = 0f, y = 0f, z = 0f;
        for (int dr = -1; dr <= 0; dr++) {
            for (int dc = -1; dc <= 0; dc++) {
                int rr = r + dr, cc = c + dc;
                if (rr < 0 || cc < 0 || rr >= rows - 1 || cc >= cols - 1) continue;
                float[] n = cellNormal[rr][cc];
                x += n[0]; y += n[1]; z += n[2];
            }
        }
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len < 1e-6f) return new float[]{0f, 0f, 1f};
        return new float[]{x / len, y / len, z / len};
    }

    /** The procedural weave colour at a normalised cloth point, including the Optifine monogram. */
    private static int weaveColor(CosmeticCatalog.Cape cape, float u, float v) {
        int clothColor = CapePatterns.colorAt(cape.pattern, u, v, cape.color, cape.accentColor);
        if (cape.pattern == CosmeticCatalog.CapePattern.OPTIFINE
                && CapePatterns.optifineMonogramAt(u, v)) {
            clothColor = cape.accentColor;
        }
        return clothColor;
    }

    private static float[] sub(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]};
    }
}
