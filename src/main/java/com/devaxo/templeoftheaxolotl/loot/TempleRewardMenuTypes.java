package com.devaxo.templeoftheaxolotl.loot;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TempleRewardMenuTypes {
    private TempleRewardMenuTypes() {}
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, TempleOfTheAxolotl.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<TempleRewardMenu>> REWARD_VAULT =
            MENUS.register("reward_vault", () -> IMenuTypeExtension.create(
                    (windowId, inventory, extraData) -> new TempleRewardMenu(windowId, inventory)));
}
