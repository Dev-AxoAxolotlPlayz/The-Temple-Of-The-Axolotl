package com.devaxo.templeoftheaxolotl.repository;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CelestialRepositoryMenuTypes {
    private CelestialRepositoryMenuTypes() {}
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TempleOfTheAxolotl.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<CelestialRepositoryMenu>> REPOSITORY =
            MENUS.register("celestial_repository", () -> IMenuTypeExtension.create((id, inv, data) -> new CelestialRepositoryMenu(id, inv)));
}
