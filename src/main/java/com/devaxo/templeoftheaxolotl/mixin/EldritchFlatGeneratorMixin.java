package com.devaxo.templeoftheaxolotl.mixin;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(FlatLevelSource.class)
public abstract class EldritchFlatGeneratorMixin {
    @Inject(method="fillFromNoise",at=@At("HEAD"),cancellable=true)
    private void tota$finite(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> ci) {
        var generator=(FlatLevelSource)(Object)this;
        if(generator.getBiomeSource().possibleBiomes().stream().noneMatch(b->b.unwrapKey().map(k->k.location().toString().equals("temple_of_the_axolotl:void_sea")).orElse(false)))return;
        var cp=chunk.getPos();
        if(cp.x < -16 || cp.x > 15 || cp.z < -16 || cp.z > 15) {ci.setReturnValue(CompletableFuture.completedFuture(chunk));return;}
    }
    @Inject(method="fillFromNoise",at=@At("RETURN"),cancellable=true)
    private void tota$boundary(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> ci) {
        var generator=(FlatLevelSource)(Object)this;
        if(generator.getBiomeSource().possibleBiomes().stream().noneMatch(b->b.unwrapKey().map(k->k.location().toString().equals("temple_of_the_axolotl:void_sea")).orElse(false)))return;
        var cp=chunk.getPos(); if(cp.x < -16 || cp.x > 15 || cp.z < -16 || cp.z > 15)return;
        ci.setReturnValue(ci.getReturnValue().thenApply(c->{var pos=new BlockPos.MutableBlockPos();for(int x=cp.getMinBlockX();x<=cp.getMaxBlockX();x++)for(int z=cp.getMinBlockZ();z<=cp.getMaxBlockZ();z++)if(x==-256||x==255||z==-256||z==255)for(int y=-63;y<=24;y++){pos.set(x,y,z);c.setBlockState(pos,Blocks.BEDROCK.defaultBlockState(),false);}return c;}));
    }
}
