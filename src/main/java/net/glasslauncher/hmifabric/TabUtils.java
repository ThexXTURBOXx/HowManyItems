package net.glasslauncher.hmifabric;

import net.glasslauncher.hmifabric.tabs.TabCrafting;
import net.glasslauncher.hmifabric.tabs.TabRegistry;
import net.glasslauncher.hmifabric.tabs.TabSmelting;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.util.Identifier;

import java.util.*;

public class TabUtils {

    private static final Map<Class<? extends HandledScreen>, ItemStack> guiToBlock = new HashMap<>();

    public static ItemStack getItemFromGui(HandledScreen screen) {
        return guiToBlock.get(screen.getClass());
    }

    public static void putItemGui(Class<? extends HandledScreen> gui, ItemStack item) {
        guiToBlock.put(gui, item);
    }

    public static void addWorkBenchGui(Class<? extends HandledScreen> gui) {
        TabCrafting workbenchTab = (TabCrafting) TabRegistry.INSTANCE.get(Identifier.of(HowManyItems.MODID, "crafting"));
        //noinspection ConstantConditions If this is null, we have bigger issues.
        workbenchTab.guiCraftingStations.add(gui);
    }

    public static void addEquivalentWorkbench(ItemStack item) {
        TabCrafting workbenchTab = (TabCrafting) TabRegistry.INSTANCE.get(Identifier.of(HowManyItems.MODID, "crafting"));
        //noinspection ConstantConditions
        workbenchTab.equivalentCraftingStations.add(item);
    }

    public static void addEquivalentFurnace(ItemStack item) {
        TabSmelting furnaceTab = (TabSmelting) TabRegistry.INSTANCE.get(Identifier.of(HowManyItems.MODID, "smelting"));
        //noinspection ConstantConditions
        furnaceTab.equivalentCraftingStations.add(item);
    }
}
