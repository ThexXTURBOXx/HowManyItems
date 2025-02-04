package net.glasslauncher.hmifabric.tabs;

import com.mojang.serialization.Lifecycle;
import net.glasslauncher.hmifabric.HowManyItems;
import net.glasslauncher.hmifabric.TabUtils;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.registry.*;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.util.Namespace;

import java.util.*;

public class TabRegistry extends SimpleRegistry<Tab> {
    private static final Tab EMPTY = new Tab(Namespace.MINECRAFT, 0, 0, 0, 0, 0) {
        @Override
        public ItemStack getTabItem() {
            return null;
        }

        @Override
        public ItemStack[][] getItems(int index, ItemStack filter) {
            return new ItemStack[0][];
        }

        @Override
        public void draw(int x, int y, int recipeOnThisPageIndex, int cursorX, int cursorY) {}

        @Override
        public Class<? extends HandledScreen> getGuiClass() {
            return null;
        }
    };
    public static final RegistryKey<Registry<Tab>> KEY = RegistryKey.ofRegistry(HowManyItems.MODID.id("tabs"));
    public static final TabRegistry INSTANCE = Registries.create(KEY, new TabRegistry(), registry -> EMPTY, Lifecycle.experimental());

    public List<Tab> tabOrder = new ArrayList<>();

    public TabRegistry() {
        super(KEY, Lifecycle.experimental(), false);
    }

    /**
     * Use this over Regsitry.register, otherwise you'll have errors and crashes.
     */
    public void register(Identifier identifier, Tab tab, ItemStack displayItem) {
        Registry.register(this, identifier, tab);
        TabUtils.putItemGui(tab.getGuiClass(), displayItem);
        tabOrder.add(tab);
    }

    public void addEquivalentCraftingStation(Identifier identifier, ItemStack displayitem) {
        //noinspection ConstantConditions
        INSTANCE.get(identifier).equivalentCraftingStations.add(displayitem);
    }

}
