package com.devaxo.templeoftheaxolotl.client.renderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.io.DataInputStream;
public final class AxoliteTridentMesh {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl","textures/item/axolite_trident.png");
    private static final ResourceLocation MESH=ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl","models/axolite_trident_mesh.bin");
    private static float[] vertices;
    public static void render(PoseStack pose, MultiBufferSource buffers,int light,int overlay) {
        if(vertices==null) {
            try(var stream=new DataInputStream(Minecraft.getInstance().getResourceManager().getResourceOrThrow(MESH).open())) {
                vertices=new float[10368];
                for(int i=0;i<vertices.length;i++) vertices[i]=stream.readFloat();
            } catch(java.io.IOException e) { vertices=null; throw new IllegalStateException("Axolite trident mesh",e); }
        }
        var consumer=buffers.getBuffer(RenderType.armorCutoutNoCull(TEXTURE));
        var p=pose.last();
        for(int i=0;i<vertices.length;i+=8) consumer.addVertex(p.pose(),vertices[i],vertices[i+1],vertices[i+2]).setColor(255,255,255,255).setUv(vertices[i+3],vertices[i+4]).setOverlay(overlay).setLight(light).setNormal(p,vertices[i+5],vertices[i+6],vertices[i+7]);
    }
}
