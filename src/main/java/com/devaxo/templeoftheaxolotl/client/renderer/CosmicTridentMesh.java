package com.devaxo.templeoftheaxolotl.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.io.DataInputStream;
import java.io.IOException;

/** UV-mapped Blockbench geometry baked from cosmic_trident.geo.json. */
public final class CosmicTridentMesh {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "textures/item/cosmic_trident.png");
    private static final ResourceLocation MESH = ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "models/cosmic_trident_mesh.bin");
    private static float[] vertices;
    private CosmicTridentMesh() {}

    public static void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (vertices == null) {
            try (var stream = new DataInputStream(Minecraft.getInstance().getResourceManager().getResourceOrThrow(MESH).open())) {
                int count = stream.readInt();
                if (count <= 0 || count > 1000000 || count % 8 != 0) throw new IOException("Invalid cosmic mesh vertex data");
                float[] loaded = new float[count];
                for (int i = 0; i < count; i++) loaded[i] = stream.readFloat();
                vertices = loaded;
            } catch (IOException e) { throw new IllegalStateException("Cosmic trident mesh", e); }
        }
        var consumer = buffers.getBuffer(RenderType.armorCutoutNoCull(TEXTURE));
        var last = pose.last();
        for (int i = 0; i < vertices.length; i += 8) {
            consumer.addVertex(last.pose(), vertices[i], vertices[i+1], vertices[i+2])
                    .setColor(255,255,255,255).setUv(vertices[i+3],vertices[i+4])
                    .setOverlay(overlay).setLight(light)
                    .setNormal(last, vertices[i+5],vertices[i+6],vertices[i+7]);
        }
    }
}
