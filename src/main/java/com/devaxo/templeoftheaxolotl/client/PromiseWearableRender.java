package com.devaxo.templeoftheaxolotl.client;
import com.devaxo.templeoftheaxolotl.promise.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.google.gson.*;
import java.util.*;
import java.io.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.player.AbstractClientPlayer;
@EventBusSubscriber(modid="temple_of_the_axolotl",value=Dist.CLIENT)
public final class PromiseWearableRender {
 private static boolean renderingBody;
 private static Vec3 bodyOffset=Vec3.ZERO;
 public static boolean isFirstPersonBody(net.minecraft.world.entity.LivingEntity entity){
   return renderingBody && entity==Minecraft.getInstance().player;
 }
 // Body offset technique adapted from First-Person Model, MIT, copyright 2019 tr7zw.
 private static Vec3 immersiveOffset(AbstractClientPlayer player,float partial){
   float yaw=net.minecraft.util.Mth.rotLerp(partial,player.yBodyRotO,player.yBodyRot);
   float pitch=net.minecraft.util.Mth.lerp(partial,player.xRotO,player.getXRot());
   double distance=player.isVisuallySwimming()?(pitch>0?.60:.50):player.isCrouching()?.27:player.isPassenger()?.20:.25;
   double angle=Math.toRadians(yaw);
   double vertical=player.isVisuallySwimming()&&pitch>0&&player.isUnderWater()?.6*Math.sin(Math.toRadians(pitch)):0;
   return new Vec3(distance*Math.sin(angle),vertical,-distance*Math.cos(angle));
 }
 @SubscribeEvent public static void hands(net.neoforged.neoforge.client.event.RenderHandEvent e){
   var mc=Minecraft.getInstance();if(mc.player!=null&&mc.options.getCameraType().isFirstPerson()&&PromiseVisualState.get(mc.player.getUUID()).equipped()&&!mc.player.isInvisible())e.setCanceled(true);
 }
 static ResourceLocation id(String p){return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl",p);}
 static final ResourceLocation TEX=id("textures/entity/axoluthus_promise.png");
 static final Map<UUID, Map<String,float[]>> POSES=new WeakHashMap<>();
 static Map<String,float[]> currentPose;
 static float blend;
 static final Map<UUID,Long> TIMES=new WeakHashMap<>();
 static JsonArray bones; static JsonObject animations; static boolean failed; static Vec3 tip; static Vec3 tangent; static Vec3 target; static Vec3 renderTarget; static PromiseRig.Chain chain; static boolean swimmingStroke; static double strokeTime;
 static JsonObject load(String path)throws IOException{try(var r=Minecraft.getInstance().getResourceManager().getResourceOrThrow(id(path)).openAsReader()){return JsonParser.parseReader(r).getAsJsonObject();}}
 static float[] vec(JsonObject o,String k){if(!o.has(k))return new float[3];var a=o.getAsJsonArray(k);return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.size()>2?a.get(2).getAsFloat():0};}
 @SubscribeEvent public static void draw(RenderPlayerEvent.Post e){
 if(e.getEntity()==Minecraft.getInstance().player&&Minecraft.getInstance().options.getCameraType().isFirstPerson())return;
 render(e.getEntity(),e.getPoseStack(),e.getMultiBufferSource(),e.getPackedLight(),e.getPartialTick(),true);
 }
 @SubscribeEvent public static void firstPerson(RenderLevelStageEvent e){
 var mc=Minecraft.getInstance();if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||mc.player==null||mc.getCameraEntity()!=mc.player||!mc.options.getCameraType().isFirstPerson())return;
 var snap=PromiseVisualState.get(mc.player.getUUID());if(!snap.equipped()||mc.player.isInvisible())return;
 float partial=e.getPartialTick().getGameTimeDeltaPartialTick(false);
 var pose=e.getPoseStack();var offset=mc.player.getPosition(partial).subtract(e.getCamera().getPosition());
 var buffers=mc.renderBuffers().bufferSource();var dispatcher=mc.getEntityRenderDispatcher();int light=dispatcher.getPackedLightCoords(mc.player,partial);
 var renderer=dispatcher.getRenderer(mc.player);
 bodyOffset=immersiveOffset(mc.player,partial);
 pose.pushPose();
 try{
   pose.translate(offset.x+bodyOffset.x,offset.y+bodyOffset.y,offset.z+bodyOffset.z);
   if(renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr){
     var model=pr.getModel(); boolean head=model.head.visible,hat=model.hat.visible;
     renderingBody=true;
     pose.pushPose();
     try{pr.render(mc.player,mc.player.getYRot(),partial,pose,buffers,light);}
     finally{pose.popPose();renderingBody=false;model.head.visible=head;model.hat.visible=hat;}
   }
   render(mc.player,pose,buffers,light,partial,true);
 }finally{bodyOffset=Vec3.ZERO;renderingBody=false;pose.popPose();}
 buffers.endBatch();
 }
 static void render(net.minecraft.world.entity.player.Player player,PoseStack pose,MultiBufferSource buffers,int light,float partial,boolean physical){
 var snap=PromiseVisualState.get(player.getUUID());if(!snap.equipped()||player.isInvisible()||failed)return;
 if(bones==null)try{bones=load("geo/axoluthus_promise.geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");animations=load("animations/axoluthus_promise.animation.json").getAsJsonObject("animations");}catch(Exception ex){failed=true;return;}
 long now=System.nanoTime(); Long previous=TIMES.put(player.getUUID(),now);
 blend=previous==null?1:(float)(1-Math.exp(-14*Math.min(.1,Math.max(0,(now-previous)*1e-9))));
 currentPose=POSES.computeIfAbsent(player.getUUID(),k->new HashMap<>());
 float yaw=180-net.minecraft.util.Mth.rotLerp(partial,player.yBodyRotO,player.yBodyRot);
 pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(yaw));
 var vc=buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));

 float scale=1+.03f*snap.upgrade();
 for(var el:bones){var b=el.getAsJsonObject();String name=b.get("name").getAsString();if(!name.contains("_Root_"))continue;pose.pushPose();float[] pivot=vec(b,"pivot");pose.translate(pivot[0]/16,pivot[1]/16,pivot[2]/16);pose.scale(scale,scale,scale);tip=null;chain=PromiseRig.CHAINS.stream().filter(c->c.root().equals(name)).findFirst().orElse(null);int slot=PromiseRig.CHAINS.indexOf(chain);if(slot<0){pose.popPose();continue;}swimmingStroke=snap.mobility()&&slot%4>=2&&player.isInWater()&&player.isSwimming();strokeTime=player.tickCount+partial;var anim=animations.getAsJsonObject(snap.enabled(slot)?"active_tentacle_idle":"dormant_tentacle_idle").getAsJsonObject("bones");Vec3 contact=snap.enabled(slot)&&!swimmingStroke?snap.target(slot):null;target=contact==null?null:contact.subtract(player.getPosition(partial)).subtract(bodyOffset).yRot((float)Math.toRadians(-yaw));if(target!=null){pose.popPose();var q=pose.last().pose().transformPosition(new org.joml.Vector3f((float)target.x,(float)target.y,(float)target.z));renderTarget=new Vec3(q.x,q.y,q.z);pose.pushPose();pose.translate(pivot[0]/16,pivot[1]/16,pivot[2]/16);pose.scale(scale,scale,scale);}drawBone(b,anim,pose,vc,light,physical && !(player==Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson() && !snap.enabled(slot)));pose.popPose(); if(tip!=null && contact!=null){var world=contact.subtract(player.getPosition(partial)).subtract(bodyOffset); var local=world.yRot((float)Math.toRadians(-yaw)); var end=pose.last().pose().transformPosition(new org.joml.Vector3f((float)local.x,(float)local.y,(float)local.z)); Vec3 endpoint=new Vec3(end.x,end.y,end.z);int face=snap.face(slot);
 if(slot%4>=2&&face>=0&&face<6){var f=net.minecraft.core.Direction.from3DDataValue(face);Vec3 n=new Vec3(f.getStepX(),f.getStepY(),f.getStepZ()).yRot((float)Math.toRadians(-yaw));var nd=pose.last().pose().transformDirection(new org.joml.Vector3f((float)n.x,(float)n.y,(float)n.z));Vec3 normal=new Vec3(nd.x,nd.y,nd.z).normalize();Vec3 neck=endpoint.add(normal.scale(.18));curve(buffers,tip,neck,tangent,true);cup(buffers,endpoint,normal,player.tickCount+partial);}
 else curve(buffers,tip,endpoint,tangent,slot%4>=2); vc=buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));}}
 pose.popPose();}
 static void drawBone(JsonObject b,JsonObject anim,PoseStack pose,VertexConsumer vc,int light,boolean physical){
 String name=b.get("name").getAsString();float[] p=vec(b,"pivot"),r=vec(b,"rotation");if(anim.has(name)){var a=anim.getAsJsonObject(name);if(a.has("rotation")&&a.get("rotation").isJsonObject()){var rr=vec(a.getAsJsonObject("rotation"),"vector");for(int i=0;i<3;i++)r[i]+=rr[i];}}
 // Root fan uses the reversed Z convention exactly once.
 if(chain!=null && name.equals(chain.root())) r[2]=(float)PromiseRig.clampZ(chain,0,-r[2]);
 // Constrained local aiming, starting at the authored pose. No physics writes.
 if(target!=null && chain!=null){
   int index=chain.bones().indexOf(name);
   if(index>=0){
     var local=new org.joml.Matrix4f(pose.last().pose()).rotateZ((float)Math.toRadians(r[2])).invert().transformPosition(new org.joml.Vector3f((float)renderTarget.x,(float)renderTarget.y,(float)renderTarget.z));
     double yaw=Math.toDegrees(Math.atan2(local.x,local.z));
     double pitch=-Math.toDegrees(Math.atan2(local.y,Math.hypot(local.x,local.z)));
     r[1]=(float)PromiseRig.clampY(chain,r[1]*.65+yaw*.35);
     r[0]=(float)PromiseRig.clampX(r[0]*.65+pitch*.35);
   }
 }

 if(swimmingStroke && chain!=null){int i=chain.bones().indexOf(name);if(i>=0){double phase=(strokeTime%24)/24.0;double wave=Math.cos(phase*2*Math.PI-i*.32);r[0]=(float)PromiseRig.clampX(5*wave);r[1]=(float)PromiseRig.clampY(chain,(chain.left()?1:-1)*(5+5*wave));r[2]=(float)PromiseRig.clampZ(chain,i,(chain.left()?1:-1)*(12+10*Math.cos(phase*2*Math.PI)));}}
 float[] old=currentPose.get(name);
 if(old!=null)for(int i=0;i<3;i++)r[i]=old[i]+net.minecraft.util.Mth.wrapDegrees(r[i]-old[i])*blend;
 currentPose.put(name,r.clone());
 pose.mulPose(Axis.ZP.rotationDegrees(r[2]));pose.mulPose(Axis.YP.rotationDegrees(r[1]));pose.mulPose(Axis.XP.rotationDegrees(r[0]));
 if(physical&&b.has("cubes"))for(var el:b.getAsJsonArray("cubes")){var c=el.getAsJsonObject();float[] o=vec(c,"origin"),s=vec(c,"size");float x=(o[0]-p[0])/16,y=(o[1]-p[1])/16,z=(o[2]-p[2])/16,X=x+s[0]/16,Y=y+s[1]/16,Z=z+s[2]/16;
 float[][] v={{x,y,z},{X,y,z},{X,Y,z},{x,Y,z},{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}};
 int[][] faces={{1,0,3,2},{5,1,2,6},{4,5,6,7},{0,4,7,3},{3,7,6,2},{0,1,5,4}};String[] names={"north","east","south","west","up","down"};float[][] normals={{0,0,-1},{1,0,0},{0,0,1},{-1,0,0},{0,1,0},{0,-1,0}};
 var uv=c.getAsJsonObject("uv");for(int f=0;f<6;f++){if(!uv.has(names[f]))continue;var face=uv.getAsJsonObject(names[f]);float[] t=vec(face,"uv"),sz=vec(face,"uv_size");for(int j=0;j<4;j++){float[] q=v[faces[f][j]],n=normals[f];vc.addVertex(pose.last().pose(),q[0],q[1],q[2]).setColor(255,255,255,255).setUv((t[0]+(j==1||j==2?sz[0]:0))/128,(t[1]+(j<2?sz[1]:0))/128).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),n[0],n[1],n[2]);}}}
 if(name.contains("_Tip_") && Integer.parseInt(name.substring(name.lastIndexOf('_')+1))%3==0 && b.has("cubes")){var c=b.getAsJsonArray("cubes").get(0).getAsJsonObject();float[] o=vec(c,"origin"),sz=vec(c,"size");var q=pose.last().pose().transformPosition(new org.joml.Vector3f((o[0]+sz[0]/2-p[0])/16,(o[1]+sz[1]/2-p[1])/16,(o[2]+sz[2]-p[2])/16));tip=new Vec3(q.x,q.y,q.z);var d=pose.last().pose().transformDirection(new org.joml.Vector3f(0,0,1));tangent=new Vec3(d.x,d.y,d.z).normalize();}
 for(var el:bones){var child=el.getAsJsonObject();if(!child.has("parent")||!child.get("parent").getAsString().equals(name))continue;float[] q=vec(child,"pivot");pose.pushPose();pose.translate((q[0]-p[0])/16,(q[1]-p[1])/16,(q[2]-p[2])/16);drawBone(child,anim,pose,vc,light,physical);pose.popPose();}}
 static void curve(MultiBufferSource buffers,Vec3 a,Vec3 b,Vec3 tangent,boolean curved){
   coloredBeam(buffers,a,b,tangent,curved,1.0,85,230,255);
 }
 public static void coloredBeam(MultiBufferSource buffers,Vec3 a,Vec3 b,Vec3 tangent,boolean curved,double thickness,int red,int green,int blue){
   beamGeometry(buffers,a,b,tangent,curved,thickness,red,green,blue,false);
 }
 public static void noodleBeam(MultiBufferSource buffers,Vec3 a,Vec3 b){
   beamGeometry(buffers,a,b,b.subtract(a).normalize(),false,2.0,22,52,190,true);
 }
 private static void beamGeometry(MultiBufferSource buffers,Vec3 a,Vec3 b,Vec3 tangent,boolean curved,double thickness,int red,int green,int blue,boolean bold){
   double length=a.distanceTo(b);if(length<.001)return;
   Vec3 chord=b.subtract(a).normalize();
   Vec3 lateral=tangent.subtract(chord.scale(tangent.dot(chord)));
   Vec3 c=curved?a.add(chord.scale(length/3)).add(lateral.scale(Math.min(.55,length*.18))):a.lerp(b,1.0/3);
   Vec3 d=b.subtract(chord.scale(length/3));
   int count=32;
   Vec3[][][] rings=new Vec3[2][count+1][8];
   Vec3 normal=null;
   for(int i=0;i<=count;i++){
     double t=(double)i/count,u=1-t;
     Vec3 q=a.scale(u*u*u).add(c.scale(3*u*u*t)).add(d.scale(3*u*t*t)).add(b.scale(t*t*t));
     Vec3 dir=c.subtract(a).scale(3*u*u).add(d.subtract(c).scale(6*u*t)).add(b.subtract(d).scale(3*t*t)).normalize();
     if(dir.lengthSqr()<.0001)dir=chord;
     if(normal!=null)normal=normal.subtract(dir.scale(normal.dot(dir))).normalize();
     if(normal==null||normal.lengthSqr()<.01)normal=dir.cross(Math.abs(dir.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0)).normalize();
     Vec3 binormal=dir.cross(normal).normalize();
     for(int layer=0;layer<2;layer++)for(int k=0;k<8;k++){
       double angle=k*Math.PI/4, radius=(layer==0?.135:.042)*thickness;
       rings[layer][i][k]=q.add(normal.scale(radius*Math.cos(angle))).add(binormal.scale(radius*Math.sin(angle)));
     }
   }
   for(int pass=0;pass<2;pass++){
     int layer=bold?1-pass:pass;
     var layerOut=bold && layer==1 ? buffers.getBuffer(RenderType.debugQuads()) : buffers.getBuffer(RenderType.lightning());
     int r=bold && layer==0?80:red, g=bold && layer==0?233:green, bl=bold && layer==0?239:blue;
     int alpha=bold?(layer==0?55:255):(layer==0?35:220);
     for(int i=0;i<count;i++)for(int k=0;k<8;k++){
       int n=(k+1)%8;
       for(Vec3 point:List.of(rings[layer][i][k],rings[layer][i][n],rings[layer][i+1][n],rings[layer][i+1][k]))
         layerOut.addVertex((float)point.x,(float)point.y,(float)point.z).setColor(r,g,bl,alpha);
     }
   }
 }
 static void cup(MultiBufferSource buffers,Vec3 center,Vec3 normal,float time){
   // Surface-aligned bell with a closed contact rim. Offset prevents coplanar flicker.
   Vec3 u=normal.cross(Math.abs(normal.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0)).normalize();
   Vec3 v=normal.cross(u).normalize();
   double[] heights={.18,.13,.065,.018,.010,.018,.045};
   double[] radii={.042,.075,.15,.235,.25,.265,.235};
   int sides=24;
   Vec3[][] rings=new Vec3[heights.length][sides];
   for(int j=0;j<heights.length;j++)for(int i=0;i<sides;i++){
     double a=i*2*Math.PI/sides;
     rings[j][i]=center.add(normal.scale(heights[j])).add(u.scale(Math.cos(a)*radii[j])).add(v.scale(Math.sin(a)*radii[j]));
   }
   var out=buffers.getBuffer(RenderType.lightning());
   int glow=(int)(190+25*Math.sin(time*.08));
   for(int j=0;j<heights.length-1;j++)for(int i=0;i<sides;i++){
     int next=(i+1)%sides;
     for(Vec3 p:List.of(rings[j][i],rings[j][next],rings[j+1][next],rings[j+1][i]))
       out.addVertex((float)p.x,(float)p.y,(float)p.z).setColor(85,230,255,j>=3?glow:100);
   }
 }
}
