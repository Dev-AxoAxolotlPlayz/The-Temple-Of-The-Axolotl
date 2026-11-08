package com.devaxo.templeoftheaxolotl.pre07;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Stationary water-looking cloud floor. Only subthermal entities can cross it. */
public final class SubthermalThermoclineBlock extends LiquidBlock {
    public static final TagKey<EntityType<?>> SUBTHERMAL = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "subthermal"));

    public SubthermalThermoclineBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    private static boolean mayCross(Entity entity) {
        if (entity == null) return false;
        if (entity.getType().is(SUBTHERMAL)) return true;
        // Explicit submarine permission is robust even when datapack tags are
        // not yet synchronized during world startup.
        if (entity instanceof com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity) return true;
        // A passenger is permitted ONLY while actually aboard an authorized vehicle.
        // This is necessary because a player still has an independent collision box.
        Entity vehicle = entity.getVehicle();
        return vehicle instanceof com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity
                || vehicle != null && vehicle.getType().is(SUBTHERMAL);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            return mayCross(entityContext.getEntity()) ? Shapes.empty() : Shapes.block();
        }
        // Non-entity queries remain solid; prevents ordinary pathfinding from routing through.
        return Shapes.block();
    }
}
