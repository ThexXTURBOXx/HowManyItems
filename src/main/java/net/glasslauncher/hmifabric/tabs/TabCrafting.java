package net.glasslauncher.hmifabric.tabs;

import com.mojang.datafixers.util.Either;
import java.util.function.Function;
import net.glasslauncher.hmifabric.Utils;
import net.glasslauncher.hmifabric.mixin.access.ShapedRecipeAccessor;
import net.glasslauncher.hmifabric.mixin.access.ShapelessRecipeAccessor;
import net.minecraft.block.Block;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.ScreenScaler;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.screen.slot.Slot;
import net.modificationstation.stationapi.api.registry.ItemRegistry;
import net.modificationstation.stationapi.api.tag.TagKey;
import net.modificationstation.stationapi.api.util.Namespace;
import net.modificationstation.stationapi.impl.recipe.StationShapedRecipe;
import net.modificationstation.stationapi.impl.recipe.StationShapelessRecipe;
import org.lwjgl.input.Keyboard;

import java.util.*;

public class TabCrafting extends TabWithTexture {

    protected List<Object> recipesComplete;
    protected List<Object> recipes;
    private final Block tabBlock;
    private boolean isVanillaWorkbench = false; //THIS IS LAZY
    public ArrayList<Class<? extends HandledScreen>> guiCraftingStations = new ArrayList<>();
    public int recipeIndex;

    private static final Random RANDOM = new Random();

    public TabCrafting(Namespace tabCreator) {
        this(tabCreator, new ArrayList<Object>(CraftingRecipeManager.getInstance().getRecipes()), Block.CRAFTING_TABLE);
        for (int i = 0; i < recipesComplete.size(); i++) {
            //Removes recipes that are too big and ruin everything @flans mod
            if (((CraftingRecipe) recipesComplete.get(i)).getSize() > 9) {
                recipesComplete.remove(i);
                i -= 1;
            }
        }
        isVanillaWorkbench = true;
        guiCraftingStations.add(CraftingScreen.class);
    }

    public TabCrafting(Namespace tabCreator, List<Object> recipesComplete, Block tabBlock) {
        this(tabCreator, 10, recipesComplete, tabBlock, "/gui/crafting.png", 118, 56, 28, 15, 56, 46, 3);
        slots[0] = new Integer[]{96, 23};
    }

    public TabCrafting(Namespace tabCreator, int slotsPerRecipe, List<Object> recipesComplete, Block tabBlock, String texturePath, int width, int height, int textureX, int textureY, int buttonX, int buttonY, int slotsWidth) {
        super(tabCreator, slotsPerRecipe, texturePath, width, height, 3, 4, textureX, textureY, buttonX, buttonY);
        this.recipesComplete = recipesComplete;
        this.tabBlock = tabBlock;
        recipes = recipesComplete;
        int i = 1;
        for (int l = 0; l < 3; l++) {
            for (int i1 = 0; i1 < slotsWidth; i1++) {
                slots[i++] = new Integer[]{2 + i1 * 18, 5 + l * 18};
            }
        }
        equivalentCraftingStations.add(getTabItem());
    }

    @Override
    public void draw(int x, int y, int recipeOnThisPageIndex, int cursorX, int cursorY) {
        super.draw(x, y, recipeOnThisPageIndex, cursorX, cursorY);
        if (recipeIndex < recipes.size() && recipes.get(recipeIndex) instanceof ShapelessRecipe) {
            Utils.bindTexture("/assets/hmifabric/textures/shapeless_icon.png");
            double size = 8;
            x += 80;
            y += 16;
            Tessellator tess = Tessellator.INSTANCE;
            tess.startQuads();
            tess.vertex(x, y + size, 0, 0, 1);
            tess.vertex(x + size, y + size, 0, 1, 1);
            tess.vertex(x + size, y, 0, 1, 0);
            tess.vertex(x, y, 0, 0, 0);
            tess.draw();
        }
    }

    @Override
    public Class<? extends HandledScreen> getGuiClass() {
        return CraftingScreen.class;
    }

    @Override
    public ItemStack[][] getItems(int index, ItemStack filter) {
        recipeIndex = index;
        ItemStack[][] items = new ItemStack[recipesPerPage][];
        for (int j = 0; j < recipesPerPage; j++) {
            items[j] = new ItemStack[slots.length];
            int k = index + j;
            if (k < recipes.size()) {
                try {
                    Object recipeObj = recipes.get(k);
                    ItemStack[] list = getIngredients(recipeObj);
                    ItemStack[] outputArray = getOutputs(recipeObj);
                    if (list == null || outputArray == null) continue;
                    System.arraycopy(outputArray, 0, items[j], 0, outputArray.length);
                    for (int j1 = 0; j1 < list.length; j1++) {
                        ItemStack item = list[j1];
                        items[j][j1 + 1] = item;
                        if (item != null && item.getDamage() == -1) {
                            if (item.hasSubtypes()) {
                                if (filter != null && item.itemId == filter.itemId) {
                                    items[j][j1 + 1] = new ItemStack(item.getItem(), 0, filter.getDamage());
                                } else {
                                    items[j][j1 + 1] = new ItemStack(item.getItem());
                                }
                            } else if (filter != null && item.itemId == filter.itemId) {
                                items[j][j1 + 1] = new ItemStack(item.getItem(), 0, filter.getDamage());
                            }
                        }
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }

            if (items[j][0] == null && recipesOnThisPage > j) {
                recipesOnThisPage = j;
                redrawSlots = true;
                break;
            }
            if (items[j][0] != null && recipesOnThisPage == j) {
                recipesOnThisPage = j + 1;
                redrawSlots = true;
            }
        }
        return items;
    }


    @Override
    public void updateRecipes(ItemStack filter, Boolean getUses) {
        List<Object> arraylist = new ArrayList<>();
        if (filter == null) {
            recipes = recipesComplete;
        } else {
            for (Object o : recipesComplete) {
                ItemStack[] list = getIngredients(o);
                ItemStack[] outputArray = getOutputs(o);
                if (list == null || outputArray == null) continue;
                if (!getUses) {
                    if (Arrays.stream(outputArray).anyMatch(itemInstance -> filter.itemId == itemInstance.itemId && (itemInstance.getDamage() == filter.getDamage() || itemInstance.getDamage() < 0 || !itemInstance.hasSubtypes()))) {
                        arraylist.add(o);
                    }
                } else {
                    try {
                        for (ItemStack itemstack1 : list) {
                            if (itemstack1 == null || filter.itemId != itemstack1.itemId || (itemstack1.hasSubtypes() && itemstack1.getDamage() != filter.getDamage()) && itemstack1.getDamage() >= 0) {
                                continue;
                            }
                            arraylist.add(o);
                            break;
                        }

                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
            recipes = arraylist;
        }
        size = recipes.size();
        super.updateRecipes(filter, getUses);
        size = recipes.size();
    }

    @Override
    public ItemStack getTabItem() {
        return new ItemStack(tabBlock);
    }

    @Override
    public Boolean drawSetupRecipeButton(Screen parent, ItemStack[] recipeItems) {
        for (Class<? extends HandledScreen> gui : guiCraftingStations) {
            if (gui.isInstance(parent)) return true;
        }
        if (isVanillaWorkbench && (parent == null || isInv(parent))) {
            for (int i = 3; i < 10; i++) {
                if (i != 4 && i != 5 && recipeItems[i] != null)
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public Boolean[] itemsInInventory(Screen parent, ItemStack[] recipeItems) {
        Boolean[] itemsInInv = new Boolean[slots.length - 1];
        List<Object> list;
        if (parent instanceof HandledScreen)
            //noinspection unchecked
            list = ((HandledScreen) parent).container.slots;
        else
            //noinspection unchecked
            list = Utils.getMC().player.currentScreenHandler.slots;
        ItemStack[] aslot = new ItemStack[list.size()];
        for (int i = 0; i < list.size(); i++) {
            if (((Slot) list.get(i)).hasStack())
                aslot[i] = ((Slot) list.get(i)).getStack().copy();
        }

        aslot[0] = null;
        recipe:
        for (int i = 1; i < recipeItems.length; i++) {
            ItemStack item = recipeItems[i];
            if (item == null) {
                itemsInInv[i - 1] = true;
                continue;
            }

            for (ItemStack slot : aslot) {
                if (slot != null && slot.count > 0 && slot.itemId == item.itemId && (slot.getDamage() == item.getDamage() || item.getDamage() < 0 || !item.hasSubtypes())) {
                    slot.count -= 1;
                    itemsInInv[i - 1] = true;
                    continue recipe;
                }
            }
            itemsInInv[i - 1] = false;
        }
        return itemsInInv;
    }

    private int recipeStackSize(List<Object> list, ItemStack[] recipeItems) {

        int[] itemStackSize = new int[recipeItems.length - 1];

        for (int i = 1; i < recipeItems.length; i++) {
            ItemStack[] aslot = new ItemStack[list.size()];
            for (int k = 0; k < list.size(); k++) {
                if (((Slot) list.get(k)).hasStack())
                    aslot[k] = ((Slot) list.get(k)).getStack().copy();
            }
            aslot[0] = null;

            ItemStack item = recipeItems[i];
            itemStackSize[i - 1] = 0;
            if (item == null) {
                itemStackSize[i - 1] = -1;
                continue;
            }
            int count = 0;
            for (ItemStack slot : aslot) {
                if (slot != null && slot.count > 0 && slot.itemId == item.itemId && (slot.getDamage() == item.getDamage() || item.getDamage() < 0 || !item.hasSubtypes())) {
                    count += slot.count;
                    slot.count = 0;
                }
            }
            int prevEqualItemCount = 1;
            for (int j = 1; j < i; j++) {
                if (recipeItems[j] != null && recipeItems[j].isItemEqual(item)) {
                    prevEqualItemCount++;
                }
            }
            for (int j = 1; j < recipeItems.length; j++) {
                if (recipeItems[j] != null && recipeItems[j].isItemEqual(item)) {
                    itemStackSize[j - 1] = count / prevEqualItemCount;
                }
            }
        }
        int finalItemStackSize = -1;
        for (int i = 0; i < itemStackSize.length; i++) {
            ItemStack item = recipeItems[i + 1];
            if (itemStackSize[i] == -1 || item.getMaxCount() == 1) continue;
            if (finalItemStackSize == -1 || itemStackSize[i] < finalItemStackSize || finalItemStackSize > item.getMaxCount()) {
                finalItemStackSize = Math.min(itemStackSize[i], item.getMaxCount());
            }
        }
        if (finalItemStackSize > 0) return finalItemStackSize;
        return 1;
    }

    @Override
    public void setupRecipe(Screen parent, ItemStack[] recipeItems) {
        if (parent == null) {
            Utils.getMC().unlockMouse();
            ScreenScaler scaledresolution = new ScreenScaler(Utils.getMC().options, Utils.getMC().displayWidth, Utils.getMC().displayHeight);
            int i = scaledresolution.getScaledWidth();
            int j = scaledresolution.getScaledHeight();
            parent = new InventoryScreen(Utils.getMC().player);
            Utils.getMC().currentScreen = parent;
            parent.init(Utils.getMC(), i, j);
            Utils.getMC().skipGameRender = false;
        }
        HandledScreen container = ((HandledScreen) parent);
        //noinspection unchecked
        List<Object> inventorySlots = container.container.slots;

        int recipeStackSize = 1;
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            recipeStackSize = recipeStackSize(inventorySlots, recipeItems);
        }

        this.player = Utils.getMC().player;
        this.inv = Utils.getMC().interactionManager;
        this.windowId = container.container.syncId;
        for (int recipeSlotIndex = 1; recipeSlotIndex < recipeItems.length; recipeSlotIndex++) {
            if (isInv(parent) && recipeSlotIndex > 5)
                break;
            int slotid = recipeSlotIndex;
            if (isInv(parent) && recipeSlotIndex > 3) {
                slotid--;
            }
            Slot recipeSlot = (Slot) inventorySlots.get(slotid);
            //clear recipe slot
            if (recipeSlot.hasStack()) {
                this.clickSlot(slotid, true, true);

                if (recipeSlot.hasStack()) {
                    this.clickSlot(slotid, true, false);
                    if (player.inventory.getCursorStack() != null) {
                        for (int j = slotid + 1; j < inventorySlots.size(); j++) {
                            Slot slot = (Slot) inventorySlots.get(j);
                            if (!slot.hasStack()) {
                                this.clickSlot(j, true, false);
                                break;
                            }
                        }
                        if (player.inventory.getCursorStack() != null) {
                            this.clickSlot(-999, true, false);
                        }
                    }
                }
            }

            //if recipe slot should be empty, continue
            ItemStack item = recipeItems[recipeSlotIndex];
            if (item == null) {
                continue;
            }

            //locate correct item and put in recipe slot
            while (!recipeSlot.hasStack() || (recipeSlot.getStack().count < recipeStackSize && recipeSlot.getStack().getMaxCount() > 1))
                for (int inventorySlotIndex = recipeSlotIndex + 1; inventorySlotIndex < inventorySlots.size(); inventorySlotIndex++) {
                    Slot inventorySlot = (Slot) inventorySlots.get(inventorySlotIndex);
                    if (inventorySlot.hasStack() && inventorySlot.getStack().itemId == item.itemId && (inventorySlot.getStack().getDamage() == item.getDamage() || item.getDamage() < 0 || !item.hasSubtypes())) {
                        this.clickSlot(inventorySlotIndex, true, false);
                        if (isInv(parent) && recipeSlotIndex > 3) {
                            this.clickSlot(recipeSlotIndex - 1, false, false);
                        } else
                            this.clickSlot(recipeSlotIndex, false, false);
                        this.clickSlot(inventorySlotIndex, true, false);
                        break;
                    }
                }

        }

    }

    InteractionManager inv;
    ClientPlayerEntity player;
    int windowId;

    void clickSlot(int slotIndex, boolean leftClick, boolean shiftClick) {
        inv.clickSlot(windowId, slotIndex, leftClick ? 0 : 1, shiftClick, player);
    }

    boolean isInv(Screen screen) {
        return screen instanceof Inventory;
    }

    private static ItemStack[] getIngredients(Object recipeObj) {
        ItemStack[] list = null;
        if (recipeObj instanceof StationShapedRecipe recipe) {
            list = new ItemStack[9];
            Either<TagKey<Item>, ItemStack>[] grid = recipe.getGrid();
            for (int h = 0; h < recipe.height; h++)
                for (int w = 0; w < recipe.width; w++) {
                    int localId = (h * recipe.width) + w;
                    Either<TagKey<Item>, ItemStack> ingredient = grid[localId];
                    if (ingredient == null) continue;
                    int id = (h * 3) + w;
                    list[id] = ingredient.map(tag -> new ItemStack(ItemRegistry.INSTANCE.getEntryList(tag).orElseThrow(
                                    () -> new RuntimeException("Identifier ingredient \"" + tag.id() + "\" has no entry in the tag registry!"))
                            .getRandom(RANDOM).orElseThrow().value()), Function.identity());
                }
        } else if (recipeObj instanceof ShapedRecipe recipe) {
            list = new ItemStack[9];
            int width = ((ShapedRecipeAccessor) recipe).getWidth();
            int height = ((ShapedRecipeAccessor) recipe).getHeight();
            ItemStack[] grid = ((ShapedRecipeAccessor) recipe).getInput();
            for (int h = 0; h < height; h++)
                for (int w = 0; w < width; w++) {
                    int localId = (h * width) + w;
                    ItemStack ingredient = grid[localId];
                    if (ingredient == null) continue;
                    int id = (h * 3) + w;
                    list[id] = ingredient;
                }
        } else if (recipeObj instanceof StationShapelessRecipe recipe) {
            Either<TagKey<Item>, ItemStack>[] ingredients = recipe.getIngredients();
            list = new ItemStack[ingredients.length];
            for (int i = 0, ingredientsLength = ingredients.length; i < ingredientsLength; i++)
                list[i] = ingredients[i].map(tag -> new ItemStack(ItemRegistry.INSTANCE.getEntryList(tag).orElseThrow(
                                () -> new RuntimeException("Identifier ingredient \"" + tag.id() + "\" has no entry in the tag registry!"))
                        .getRandom(RANDOM).orElseThrow().value()), Function.identity());
        } else if (recipeObj instanceof ShapelessRecipe recipe) {
            list = ((ShapelessRecipeAccessor) recipe).getInput().toArray(new ItemStack[0]);
        }
        return list;
    }

    private static ItemStack[] getOutputs(Object recipeObj) {
        ItemStack[] outputArray = null;
        if (recipeObj instanceof CraftingRecipe recipe) {
            outputArray = new ItemStack[] {recipe.getOutput() };
        }
        return outputArray;
    }

}
