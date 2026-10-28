package com.devaxo.templeoftheaxolotl.pre07;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Client-only fluid visuals registered through the supported NeoForge event. */
public final class Pre07FluidClientExtensions {
    private Pre07FluidClientExtensions() {}

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(visuals("pure_ichor_still", "pure_ichor_flow"), Pre07Fluids.PURE_TYPE.get());
        event.registerFluidType(visuals("diluted_ichor_still", "diluted_ichor_flow"), Pre07Fluids.DILUTED_TYPE.get());
    }

    private static IClientFluidTypeExtensions visuals(String still, String flowing) {
        return new IClientFluidTypeExtensions() {
            @Override public int getTintColor() { return 0xFFFFFFFF; }
            @Override public ResourceLocation getStillTexture() {
                return ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "block/" + still);
            }
            @Override public ResourceLocation getFlowingTexture() {
                return ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "block/" + flowing);
            }
        };
    }
}
