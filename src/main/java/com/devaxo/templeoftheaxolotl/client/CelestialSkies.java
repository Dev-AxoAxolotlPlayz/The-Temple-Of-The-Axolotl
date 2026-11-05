package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.BossWrathEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import org.joml.Matrix4f;
import java.util.Random;

/** Celestial currents on a world-oriented sky hemisphere, not a fullscreen tint.
 * Keeps the existing Starry Seas shader untouched. The sky uses its layered wave
 * mathematics and a separate geometry pass, with white stars instead of teal ones.
 */
public final class CelestialSkies {
    private CelestialSkies() {}
    private static final float RADIUS = 110f;

    // Deterministic, stationary world-space sky features. Generated once, not per frame.
    private static final int END_STARS = 850;
    private static final float[][] END_STAR_DATA = createEndStars();
    private static float[][] createEndStars() {
        Random r = new Random(0x7EAD507L);
        float[][] data = new float[END_STARS][6];
        for (int i = 0; i < data.length; i++) {
            double az = r.nextDouble() * Math.PI * 2;
            double el = 0.07 + r.nextDouble() * 1.45;
            data[i][0] = (float)(Math.cos(az)*Math.cos(el)*RADIUS);
            data[i][1] = (float)(Math.sin(el)*RADIUS);
            data[i][2] = (float)(Math.sin(az)*Math.cos(el)*RADIUS);
            data[i][3] = 0.035f + r.nextFloat() * 0.065f;
            data[i][4] = 0.5f + r.nextFloat() * 1.6f; // independent twinkle rate
            data[i][5] = r.nextFloat() * 6.283185f; // independent phase
        }
        return data;
    }

    // Round 10: static, pre-authored RGBA galaxy textures on fixed world-space sky planes.
    // No full-dome procedural nebula and no per-frame galaxy math.
    private static final int END_GALAXIES = 12;
    private static final net.minecraft.resources.ResourceLocation[] GALAXY_TEXTURES = {
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("templeoftheaxolotl", "textures/environment/galaxy_0.png"),
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("templeoftheaxolotl", "textures/environment/galaxy_1.png"),
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("templeoftheaxolotl", "textures/environment/galaxy_2.png"),
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("templeoftheaxolotl", "textures/environment/galaxy_3.png"),
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("templeoftheaxolotl", "textures/environment/galaxy_4.png"),
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("templeoftheaxolotl", "textures/environment/galaxy_5.png")
    };
    // x/y/z for four corners, precomputed at class initialization.
    private static final float[][][] GALAXY_QUADS = createGalaxyQuads();

    private static float[][][] createGalaxyQuads() {
        Random r = new Random(0x8A1A71L);
        float[][][] quads = new float[END_GALAXIES][4][3];
        for (int g = 0; g < END_GALAXIES; g++) {
            double az = (g + 0.28 + r.nextDouble() * 0.38) * Math.PI * 2 / END_GALAXIES;
            double el = 0.24 + r.nextDouble() * 1.04;
            float nx = (float)(Math.cos(az)*Math.cos(el));
            float ny = (float)Math.sin(el);
            float nz = (float)(Math.sin(az)*Math.cos(el));
            float ux = -nz, uy = 0f, uz = nx;
            float ulen = (float)Math.sqrt(ux*ux+uz*uz);
            ux/=ulen; uz/=ulen;
            float vx = ny*uz, vy = nz*ux-nx*uz, vz = -ny*ux;
            double rotation = r.nextDouble()*Math.PI*2;
            float cr=(float)Math.cos(rotation), sr=(float)Math.sin(rotation);
            float tx=ux*cr+vx*sr, ty=uy*cr+vy*sr, tz=uz*cr+vz*sr;
            float wx=-ux*sr+vx*cr, wy=-uy*sr+vy*cr, wz=-uz*sr+vz*cr;
            // Wide range: two prominent galaxies, four mid-distance, six distant.
            // Half-width measured in world units on a radius-110 sky.
            float halfSize = g < 2 ? 24f+r.nextFloat()*7f
                    : g < 6 ? 9f+r.nextFloat()*6f : 3.0f+r.nextFloat()*4f;
            // A mild inclination is baked into the texture itself, not the quad.
            for (int i=0;i<4;i++) {
                float a = (i==0||i==3) ? -halfSize : halfSize;
                float b = (i==0||i==1) ? -halfSize : halfSize;
                quads[g][i][0]=nx*(RADIUS*0.985f)+tx*a+wx*b;
                quads[g][i][1]=ny*(RADIUS*0.985f)+ty*a+wy*b;
                quads[g][i][2]=nz*(RADIUS*0.985f)+tz*a+wz*b;
            }
        }
        return quads;
    }

    private static void galaxyCorner(BufferBuilder b, Matrix4f m, float[] p, float u, float v) {
        b.addVertex(m,p[0],p[1],p[2]).setUv(u,v).setColor(255,255,255,255);
    }

    private static void drawEndGalaxies(Matrix4f matrix) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        for (int g=0;g<END_GALAXIES;g++) {
            RenderSystem.setShaderTexture(0,GALAXY_TEXTURES[g%GALAXY_TEXTURES.length]);
            BufferBuilder b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            float[][] q=GALAXY_QUADS[g];
            galaxyCorner(b,matrix,q[0],0f,1f);
            galaxyCorner(b,matrix,q[1],1f,1f);
            galaxyCorner(b,matrix,q[2],1f,0f);
            galaxyCorner(b,matrix,q[3],0f,0f);
            BufferUploader.drawWithShader(b.buildOrThrow());
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    public static void renderSky(Matrix4f skyViewMatrix, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !CelestialWaterMode.enabled()) return;
        boolean end = mc.level.dimension() == Level.END;
        if (!end && mc.level.dimension() != Level.OVERWORLD) return;
        if (!mc.level.getEntitiesOfClass(BossWrathEntity.class,
                mc.player.getBoundingBox().inflate(96), BossWrathEntity::isSecondFall).isEmpty()) return;

        double day = (mc.level.getDayTime() % 24000L) / 24000.0;
        float opacity = end ? 1f : clamp(Math.min((day - 0.50) / 0.07, (1.0 - day) / 0.08));
        if (!end) opacity *= 1f - Math.max(mc.level.getRainLevel(partialTick),
                mc.level.getThunderLevel(partialTick));
        if (opacity < 0.01f) return;

        // Sky geometry must NOT inherit the level's world-space camera translation.
        // The GPU's view matrix already provides camera orientation. An identity
        // model matrix puts this dome at the view origin, just like vanilla sky
        // geometry; no screen-space quad or camera-facing billboard is involved.
        Matrix4f matrix = new Matrix4f(skyViewMatrix);
        matrix.m30(0f).m31(0f).m32(0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        // Vanilla End sky renders a background cube that may have populated
        // the depth buffer. Our dome is a SKY element, not world geometry:
        // it must draw over that cube, while terrain is rendered afterwards.
        // Restrict depth-test changes to the End and restore them in finally.
        if (end) RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try {
            // Static spiral galaxies are precomputed once; no full-dome nebula mesh.
            // The Overworld aurora path below is unchanged.
            double time = mc.level.getGameTime() + partialTick;
            if (end) {
                drawEndGalaxies(matrix);
            } else {
                BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
// Low-frequency waves reuse the same multi-sine concept as Starry Seas.
            // World azimuth and altitude, not screen UV, determine the flowing pattern.
            for (int band = 0; band < 3; band++) {
                for (int i = 0; i < 192; i++) {
                    float u0 = i / 192f, u1 = (i + 1) / 192f;
                    for (int layer = 0; layer < 24; layer++) {
                        float v0 = layer / 24f, v1 = (layer + 1) / 24f;
                        float[] p00 = point(u0,v0,band,time);
                        float[] p01 = point(u0,v1,band,time);
                        float[] p10 = point(u1,v0,band,time);
                        float[] p11 = point(u1,v1,band,time);
                        int[] rgb = color(end,band,layer);
                        float edge0 = (float)Math.pow(Math.sin(Math.PI * u0), 1.2);
                        float edge1 = (float)Math.pow(Math.sin(Math.PI * u1), 1.2);
                        float vertical0 = (float)Math.sin(Math.PI * v0);
                        float vertical1 = (float)Math.sin(Math.PI * v1);
                        int a00 = (int)(opacity * 100 * edge0 * vertical0);
                        int a01 = (int)(opacity * 100 * edge0 * vertical1);
                        int a10 = (int)(opacity * 100 * edge1 * vertical0);
                        int a11 = (int)(opacity * 100 * edge1 * vertical1);
                        vertex(b,matrix,p00,rgb,a00);
                        vertex(b,matrix,p01,rgb,a01);
                        vertex(b,matrix,p11,rgb,a11);
                        vertex(b,matrix,p00,rgb,a00);
                        vertex(b,matrix,p11,rgb,a11);
                        vertex(b,matrix,p10,rgb,a10);
                    }
                }
            }
                BufferUploader.drawWithShader(b.buildOrThrow());
            }
// A separate triangle buffer is necessary for true radial alpha falloff.
            // The old two flat quads produced square paint splatters and depth artifacts.
            BufferBuilder stars = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            Random rng = end ? null : new Random(0xA80107);
            for (int i = 0; i < (end ? END_STARS : 750); i++) {
                if (end) {
                    float[] s = END_STAR_DATA[i];
                    float shimmer = (float)(0.82 + 0.18*Math.sin(time*0.025*s[4] + s[5]));
                    softStar(stars,matrix,s[0],s[1],s[2],s[3],(int)(shimmer*215));
                    continue;
                }
                double az = rng.nextDouble() * Math.PI * 2;
                double el = 0.09 + rng.nextDouble() * 1.42;
                float x = (float)(Math.cos(az)*Math.cos(el)*RADIUS);
                float y = (float)(Math.sin(el)*RADIUS);
                float z = (float)(Math.sin(az)*Math.cos(el)*RADIUS);
                // Mostly pinpoints, with only occasional slightly brighter stars.
                // Fewer stars with a restrained three-tier size distribution.
                float radius = (i % 20 == 0 ? 0.105f * 1.50f
                        : i % 4 == 0 ? 0.038f * 1.25f : 0.038f * 1.10f)
                        + rng.nextFloat() * 0.027f;
                float shimmer = (float)(0.94 + 0.06*Math.sin(time*0.009 + i*3.71));
                int alpha = (int)(opacity * shimmer * Math.min(255, (end ? 165 : 115) + rng.nextInt(end ? 90 : 105)));
                softStar(stars,matrix,x,y,z,radius,alpha);
            }
            BufferUploader.drawWithShader(stars.buildOrThrow());
        } finally {
            RenderSystem.depthMask(true);
            if (end) RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();

        }
    }

    private static float[] point(float u,float v,int band,double ticks) {
        double az = -2.7 + u*5.4 + band*0.12;
        double wave = 0.085*Math.sin(u*13.0 - ticks*0.004 + band*1.5)
                    + 0.045*Math.sin(u*27.0 + ticks*0.0028 + band*2.1)
                    + 0.020*Math.sin(u*43.0 - ticks*0.0019);
        double base = 0.27 + band*0.085 + wave;
        double el = base + v*(0.26 + 0.05*Math.sin(u*9 + ticks*0.002));
        return new float[] {(float)(Math.sin(az)*Math.cos(el)*RADIUS),
                (float)(Math.sin(el)*RADIUS),
                (float)(-Math.cos(az)*Math.cos(el)*RADIUS)};
    }

    private static int[] color(boolean end,int band,int layer) {
        if (end) return band%2==0 ? new int[]{151,105,238} : new int[]{197,145,245};
        return band%2==0 ? new int[]{112,213,225} : new int[]{190,159,229};
    }

    private static void vertex(BufferBuilder b,Matrix4f m,float[] p,int[] c,int a) {
        b.addVertex(m,p[0],p[1],p[2]).setColor(c[0],c[1],c[2],Math.max(0,Math.min(255,a)));
    }

    private static void softStar(BufferBuilder b, Matrix4f m, float x,float y,float z,float r,int alpha) {
        // Construct tangent-plane geometry on the sky sphere, never a screen-facing quad.
        float len = (float)Math.sqrt(x*x+y*y+z*z);
        float nx=x/len, ny=y/len, nz=z/len;
        float ax=-nz, ay=0, az=nx;
        float al=(float)Math.sqrt(ax*ax+az*az);
        ax/=al;az/=al;
        float bx=ny*az, by=nz*ax-nx*az, bz=-ny*ax;
        int segments=8;
        for (int k=0;k<segments;k++) {
            double a0=k*Math.PI*2/segments,a1=(k+1)*Math.PI*2/segments;
            float s0=(float)Math.cos(a0)*r*2.5f,t0=(float)Math.sin(a0)*r*2.5f;
            float s1=(float)Math.cos(a1)*r*2.5f,t1=(float)Math.sin(a1)*r*2.5f;
            b.addVertex(m,x,y,z).setColor(255,255,255,alpha);
            b.addVertex(m,x+ax*s0+bx*t0,y+ay*s0+by*t0,z+az*s0+bz*t0).setColor(235,240,255,0);
            b.addVertex(m,x+ax*s1+bx*t1,y+ay*s1+by*t1,z+az*s1+bz*t1).setColor(235,240,255,0);
        }
    }

    private static float clamp(double v) { return (float)Math.max(0,Math.min(1,v)); }
}
