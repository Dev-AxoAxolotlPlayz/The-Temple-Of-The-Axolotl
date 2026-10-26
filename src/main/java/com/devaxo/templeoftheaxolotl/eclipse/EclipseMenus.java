package com.devaxo.templeoftheaxolotl.eclipse;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EclipseMenus {
    private EclipseMenus() {}
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, TempleOfTheAxolotl.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<EclipseMenu>> TRANSMUTATION =
            MENUS.register("celestial_transmutation", () -> IMenuTypeExtension.create(
                    (id, inv, buf) -> new EclipseMenu(id, inv, buf.readBlockPos(), false)));
    public static final DeferredHolder<MenuType<?>, MenuType<EclipseMenu>> INSCRIPTION =
            MENUS.register("celestial_inscription", () -> IMenuTypeExtension.create(
                    (id, inv, buf) -> new EclipseMenu(id, inv, buf.readBlockPos(), true)));
}
