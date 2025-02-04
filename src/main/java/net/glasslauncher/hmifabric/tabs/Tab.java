package net.glasslauncher.hmifabric.tabs;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.util.Namespace;

import java.util.*;

public abstract class Tab {

    public Tab(Namespace tabCreator, int slotsPerRecipe, int width, int height, int minPaddingX, int minPaddingY) {
        slots = new Integer[slotsPerRecipe][];
        WIDTH = width;
        HEIGHT = height;
        MIN_PADDING_X = minPaddingX;
        MIN_PADDING_Y = minPaddingY;
        TAB_CREATOR = tabCreator;
    }

    public abstract ItemStack getTabItem();

    public abstract ItemStack[][] getItems(int index, ItemStack filter);

    public int size;

    public void updateRecipes(ItemStack filter, Boolean getUses) {
        if (size == 0 && getUses) {
            for (ItemStack craftingStation : equivalentCraftingStations) {
                if (filter.itemId == craftingStation.itemId && filter.getDamage() == craftingStation.getDamage()) {
                    updateRecipes(null, getUses);
                    break;
                }
            }
        }
    }

    ;

    public abstract void draw(int x, int y, int recipeOnThisPageIndex, int cursorX, int cursorY);

    public String name() {
        return TranslationStorage.getInstance().getClientTranslation(getTabItem().getTranslationKey());
    }

    public abstract Class<? extends HandledScreen> getGuiClass();

    public ArrayList<ItemStack> equivalentCraftingStations = new ArrayList<>();

    public int index = -2;
    public int recipesPerPage = 1;
    public Boolean redrawSlots = false;
    public int recipesOnThisPage = 1;
    public int lastIndex = 0;

    public int autoX = 1;
    public int autoY = 2;

    public final Namespace TAB_CREATOR;

    public Integer[][] slots;

    public final int WIDTH;
    public final int HEIGHT;
    public final int MIN_PADDING_X;
    public final int MIN_PADDING_Y;

}
