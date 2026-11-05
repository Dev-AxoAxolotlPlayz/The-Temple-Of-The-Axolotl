package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.entity.BossWrathEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * Direction-fixed auroral curtains INSIDE vanilla LevelRenderer.renderSky.
 * This renderer never registers a RenderLevelStageEvent, never draws a screen
 * rectangle, and never modifies the camera or Starry Seas water shader.
 */
public final class CelestialAuroraSky {
    private static final int STEPS = 64;
    private static final int HEIGHT_STEPS = 8;

    private static final int VERTICES = (STEPS + 1) * (HEIGHT_STEPS + 1);
    // Scratch arrays used only when computing a new simulation snapshot.
    private static final float[] PX = new float[VERTICES];
    private static final float[] PY = new float[VERTICES];
    private static final float[] PZ = new float[VERTICES];
    private static final int[] COLOR = new int[VERTICES];

    // Two cached snapshots, each holding all five ribbons. Interpolate at render FPS.
    private static final int TOTAL_VERTICES = 5 * VERTICES;
    private static float[] oldX = new float[TOTAL_VERTICES], oldY = new float[TOTAL_VERTICES], oldZ = new float[TOTAL_VERTICES];
    private static float[] newX = new float[TOTAL_VERTICES], newY = new float[TOTAL_VERTICES], newZ = new float[TOTAL_VERTICES];
    private static int[] oldColor = new int[TOTAL_VERTICES], newColor = new int[TOTAL_VERTICES];
    private static long cachedTick = Long.MIN_VALUE;
    private static Level cachedLevel;

    // Static geometry envelopes: computed once at class initialization.
    private static final float[] U_ENDS = new float[STEPS + 1];
    private static final double[] U_ENVELOPE = new double[STEPS + 1];
    private static final float[] V_FEATHER = new float[HEIGHT_STEPS + 1];
    static {
        for (int i = 0; i <= STEPS; i++) {
            double u = (double)i / STEPS;
            double sine = Math.max(0.0, Math.sin(Math.PI * u));
            U_ENDS[i] = smooth((float)sine);
            U_ENVELOPE[i] = Math.pow(sine, 0.7);
        }
        for (int j = 0; j <= HEIGHT_STEPS; j++) {
            double v = (double)j / HEIGHT_STEPS;
            V_FEATHER[j] = (float)Math.pow(Math.max(0.0, Math.sin(Math.PI * v)), 0.85);
        }
    }

    private CelestialAuroraSky() {}

    public static void draw(Matrix4f vanillaSkyView, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (!CelestialAuroraMode.enabled() || mc.level == null || mc.player == null
                || mc.level.dimension() != Level.OVERWORLD) return;

        // Second Fall's established crimson sky takes precedence.
        if (!mc.level.getEntitiesOfClass(BossWrathEntity.class,
                mc.player.getBoundingBox().inflate(96), BossWrathEntity::isSecondFall).isEmpty()) return;

        float timeOfDay = (mc.level.getDayTime() % 24000L) / 24000.0F;
        float fadeIn = smooth((timeOfDay - 0.52F) / 0.075F);
        float fadeOut = smooth((1.0F - timeOfDay) / 0.085F);
        float weather = 1.0F - Math.max(mc.level.getRainLevel(partialTick),
                mc.level.getThunderLevel(partialTick));
        float visibility = fadeIn * fadeOut * weather;
        if (visibility < 0.015F) return;

        // This is the view matrix Minecraft passes into its sky renderer.
        // It includes VIEW ROTATION, not a camera-facing billboard transform.
        // The aurora's azimuth and elevation remain fixed in celestial space.
        Matrix4f sky = new Matrix4f(vanillaSkyView);
        // Original 1x oscillation. Refresh trig-heavy geometry at 20 Hz, not render FPS.
        long tick = mc.level.getGameTime();
        if (cachedLevel != mc.level) {
            cachedLevel = mc.level;
            cachedTick = Long.MIN_VALUE;
        }
        ensureSnapshots(tick);
        float blend = Math.max(0.0F, Math.min(1.0F, partialTick));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try {
            BufferBuilder buffer = Tesselator.getInstance().begin(
                    VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            // Every render frame only interpolates cached snapshots and submits triangles.
            // The expensive sin/cos/pow mesh calculations run at most once per game tick.
            for (int band = 0; band < 5; band++) {
                int stride = HEIGHT_STEPS + 1;
                int offset = band * VERTICES;
                for (int i = 0; i < STEPS; i++) {
                    for (int j = 0; j < HEIGHT_STEPS; j++) {
                        int a = offset + i * stride + j;
                        int b = offset + (i + 1) * stride + j;
                        emitInterpolated(buffer, sky, a, blend, visibility);
                        emitInterpolated(buffer, sky, a + 1, blend, visibility);
                        emitInterpolated(buffer, sky, b + 1, blend, visibility);
                        emitInterpolated(buffer, sky, a, blend, visibility);
                        emitInterpolated(buffer, sky, b + 1, blend, visibility);
                        emitInterpolated(buffer, sky, b, blend, visibility);
                    }
                }
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static void ensureSnapshots(long tick) {
        if (cachedTick == tick) return;
        if (cachedTick != Long.MIN_VALUE && tick == cachedTick + 1) {
            // Reuse last tick's next-frame snapshot instead of computing it twice.
            float[] temp;
            temp = oldX; oldX = newX; newX = temp;
            temp = oldY; oldY = newY; newY = temp;
            temp = oldZ; oldZ = newZ; newZ = temp;
            int[] colors = oldColor; oldColor = newColor; newColor = colors;
            buildSnapshot(tick + 1, newX, newY, newZ, newColor);
        } else {
            // World change, time command, or renderer reactivation after a pause.
            buildSnapshot(tick, oldX, oldY, oldZ, oldColor);
            buildSnapshot(tick + 1, newX, newY, newZ, newColor);
        }
        cachedTick = tick;
    }

    private static void buildSnapshot(long tick, float[] x, float[] y, float[] z, int[] colors) {
        for (int band = 0; band < 5; band++) {
            int offset = band * VERTICES;
            for (int i = 0; i <= STEPS; i++) {
                float u = (float)i / STEPS;
                // Visibility is applied at draw time, so daylight/weather fades remain smooth.
                prepareColumn(u, band, (float)tick, 1.0F, i);
                for (int j = 0; j <= HEIGHT_STEPS; j++) {
                    float v = (float)j / HEIGHT_STEPS;
                    int k = i * (HEIGHT_STEPS + 1) + j;
                    calculatePosition(u, v, band, (float)tick, k);
                    calculateColor(k, band, columnOpacity * V_FEATHER[j]);
                    int target = offset + k;
                    x[target] = PX[k]; y[target] = PY[k]; z[target] = PZ[k];
                    colors[target] = COLOR[k];
                }
            }
        }
    }

    private static void emitInterpolated(BufferBuilder b, Matrix4f matrix, int index, float blend, float visibility) {
        float x = oldX[index] + (newX[index] - oldX[index]) * blend;
        float y = oldY[index] + (newY[index] - oldY[index]) * blend;
        float z = oldZ[index] + (newZ[index] - oldZ[index]) * blend;
        int c0 = oldColor[index], c1 = newColor[index];
        int r = Math.round(((c0 >>> 24) & 255) + (((c1 >>> 24) & 255) - ((c0 >>> 24) & 255)) * blend);
        int g = Math.round(((c0 >>> 16) & 255) + (((c1 >>> 16) & 255) - ((c0 >>> 16) & 255)) * blend);
        int bl = Math.round(((c0 >>> 8) & 255) + (((c1 >>> 8) & 255) - ((c0 >>> 8) & 255)) * blend);
        int a = Math.max(0, Math.min(255, Math.round(((c0 & 255) + ((c1 & 255) - (c0 & 255)) * blend) * visibility)));
        b.addVertex(matrix, x, y, z).setColor(r, g, bl, a);
    }

    // Column parameters are shared by all rows of the same ribbon section.
    private static double baseX, baseZ, baseLower, baseHeight, baseFold;
    private static double baseEnvelope, basePhase, baseSweeping, baseSlow;
    private static double groupCos, groupSin, groupScale, groupYOffset;
    private static float columnOpacity;

    private static void prepareColumn(float u, int band, float t, float visibility, int column) {
        int group = band < 2 ? 0 : band < 4 ? 1 : 2;
        int local = group == 0 ? band : group == 1 ? band - 2 : band - 4;
        baseSlow = t * 0.00145 + local * 1.37 + group * 2.31;
        baseEnvelope = U_ENVELOPE[column];
        baseX = -72.0 + 144.0 * u + (local == 1 ? -18.0 : local == 2 ? 22.0 : 0.0);
        baseSweeping = 4.6 * Math.sin(u * 7.0 - baseSlow * 0.55)
                     + 2.2 * Math.sin(u * 14.0 + baseSlow * 0.34);
        baseLower = 24.0 + baseSweeping * 0.58 + (local == 1 ? 8.0 : local == 2 ? -6.0 : 0.0);
        baseHeight = Math.max(6.0, (local == 0 ? 17.0 : local == 1 ? 14.0 : 12.0)
                   + 6.5 * Math.sin(u * 8.0 - baseSlow * 0.47)
                   + 3.0 * Math.sin(u * 19.0 + baseSlow * 0.62));
        baseFold = Math.sin(u * 33.0 - baseSlow * 1.4)
                 + 0.45 * Math.sin(u * 57.0 + baseSlow * 0.8);
        baseZ = -58.0 + (local == 1 ? -15.0 : local == 2 ? 13.0 : 0.0)
              + baseSweeping * 0.72;
        double angle = group == 1 ? 1.92 : group == 2 ? -1.88 : 0.0;
        groupCos = Math.cos(angle);
        groupSin = Math.sin(angle);
        groupScale = group == 0 ? 1.0 : group == 1 ? 0.78 : 0.63;
        groupYOffset = group == 1 ? 6.0 : group == 2 ? -2.0 : 0.0;

        double phase = u * 33.0 - t * 0.0020 + local * 1.73 + group * 2.31;
        float pleat = (float)Math.pow(0.5 + 0.5 * Math.cos(phase), 3.0);
        float fine = (float)(0.5 + 0.5 * Math.cos(u * 69.0 + t * 0.0011));
        float shimmer = (float)(0.86 + 0.14 * Math.sin(u * 12.0 - t * 0.0013));
        columnOpacity = visibility * U_ENDS[column] * shimmer
                * (21.0F + 66.0F * pleat + 14.0F * fine)
                * (local == 0 ? 1.0F : local == 1 ? 0.65F : 0.55F)
                * (group == 0 ? 1.0F : group == 1 ? 0.78F : 0.58F);
    }

    private static void calculatePosition(float u, float v, int band, float t, int index) {
        double z = baseZ + baseEnvelope * (5.0 * baseFold * (0.25 + 0.75 * v)
                 + 1.8 * Math.sin(u * 17.0 - baseSlow * 0.6 + v * 4.0));
        double y = baseLower + v * baseHeight
                 + baseEnvelope * 1.5 * Math.sin(u * 24.0 - baseSlow + v * 2.0);
        PX[index] = (float)((baseX * groupCos - z * groupSin) * groupScale);
        PY[index] = (float)(y * groupScale + groupYOffset);
        PZ[index] = (float)((baseX * groupSin + z * groupCos) * groupScale);
    }

    private static void calculateColor(int index, int band, float alpha) {
        double wave = Math.sin((PX[index] + 72.0) / 144.0 * 8.0
                + PY[index] * 0.08);
        int group = band < 2 ? 0 : band < 4 ? 1 : 2;
        int local = group == 0 ? band : group == 1 ? band - 2 : band - 4;
        double violet = Math.max(0.0, Math.min(1.0, 0.5 + 0.5 * wave
                + (local == 1 ? 0.24 : local == 2 ? -0.15 : 0.0)
                + (group == 1 ? 0.16 : group == 2 ? -0.12 : 0.0)));
        int red = (int)(75 + 78 * violet);
        int green = (int)(210 - 63 * violet);
        int blue = (int)(223 + 19 * violet);
        int a = Math.max(0, Math.min(255, Math.round(alpha)));
        COLOR[index] = (red << 24) | (green << 16) | (blue << 8) | a;
    }

    private static float smooth(float x) {
        x = Math.max(0.0F,Math.min(1.0F,x));
        return x*x*(3.0F-2.0F*x);
    }
}
