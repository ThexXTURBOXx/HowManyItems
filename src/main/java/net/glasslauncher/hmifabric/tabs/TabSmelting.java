package net.glasslauncher.hmifabric.tabs;

import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.SmeltingRecipeManager;
import net.modificationstation.stationapi.api.recipe.FuelRegistry;
import net.modificationstation.stationapi.api.registry.ItemRegistry;
import net.modificationstation.stationapi.api.tag.TagKey;
import net.modificationstation.stationapi.api.util.Namespace;

import java.util.*;
import java.util.concurrent.*;

public class TabSmelting extends TabWithTexture {
    private static final Random RANDOM = new Random();
    public final Class<?> guiClass = TabSmelting.class;
    protected Map recipesComplete;
    protected ArrayList<Object[]> recipes = new ArrayList<>();
    private Block tabBlock;
    private int metadata;
    private boolean damagedFurnaceInput = false;

    public TabSmelting(Namespace tabCreator) {
        this(tabCreator, new HashMap(), "/gui/furnace.png", Block.FURNACE);

        recipesComplete = SmeltingRecipeManager.getInstance().getRecipes();

		/*
		for(ItemBase item: ItemBase.byId) {
			if(item != null && ModLoader.AddAllFuel(item.id) > 0)
				fuels.add(new ItemInstance(item));
		}*/

        try {
            //ModLoader.getPrivateValue(net.minecraft.src.TileEntityFurnace.class, new TileEntityFurnace(), "furnaceHacks");
            damagedFurnaceInput = true;
        } catch (Exception exception) {
            damagedFurnaceInput = false;
        }
    }

    public TabSmelting(Namespace tabCreator, Map recipes, String texturePath, Block tabBlock) {
        this(tabCreator, recipes, texturePath, tabBlock, 0);
    }

    public TabSmelting(Namespace tabCreator, Map recipes, String texturePath, Block tabBlock, int metadata) {
        this(tabCreator, 3, recipes, texturePath, 84, 56, 54, 15, tabBlock, metadata);
    }

    public TabSmelting(Namespace tabCreator, int slotsPerRecipe, Map recipes, String texturePath, int width, int height, int textureX, int textureY, Block tabBlock, int metadata) {
        this(tabCreator, slotsPerRecipe, texturePath, width, height, textureX, textureY, tabBlock, metadata);

        this.recipesComplete = recipes;
    }

    public TabSmelting(Namespace tabCreator, int slotsPerRecipe, String texturePath, int width, int height, int textureX, int textureY, Block tabBlock, int metadata) {
        super(tabCreator, slotsPerRecipe, texturePath, width, height, 3, 3, textureX, textureY);

        this.tabBlock = tabBlock;
        this.metadata = metadata;

        slots[0] = new Integer[]{62, 23};
        slots[1] = new Integer[]{2, 5};
        if (slotsPerRecipe > 2)
            slots[2] = new Integer[]{2, 41};
        equivalentCraftingStations.add(getTabItem());
    }

    @Override
    public ItemStack[][] getItems(int index, ItemStack filter) {
        ItemStack[][] items = new ItemStack[recipesPerPage][];
        for (int j = 0; j < recipesPerPage; j++) {
            items[j] = new ItemStack[slots.length];
            int k = index + j;
            if (k < recipes.size()) {
                Object[] recipeObj = recipes.get(k);
                if (recipeObj[1] instanceof ItemStack[]) {
                    ItemStack[] recipe = (ItemStack[]) recipeObj[1];
                    for (int i = 0; i < recipe.length; i++) {
                        int offset = i+1;
                        items[j][offset] = recipe[i];
                        if (recipe[i] != null && recipe[i].getDamage() == -1) {
                            if (recipe[i].hasSubtypes()) {
                                if (filter != null && recipe[i].itemId == filter.itemId) {
                                    items[j][offset] = new ItemStack(recipe[i].getItem(), 0, filter.getDamage());
                                } else {
                                    items[j][offset] = new ItemStack(recipe[i].getItem());
                                }
                            } else if (filter != null && recipe[i].itemId == filter.itemId) {
                                items[j][offset] = new ItemStack(recipe[i].getItem(), 0, filter.getDamage());
                            }
                        }
                    }
                } else if (recipeObj[1] instanceof TagKey<?>[]) {
                    //noinspection unchecked shut
                    TagKey<Item>[] recipe = (TagKey<Item>[]) recipeObj[1];
                    for (int i = 0; i < recipe.length; i++) {
                        int offset = i+1;
                        Item theHolyOneLiner = ItemRegistry.INSTANCE.getOrCreateEntryList(recipe[i]).getRandom(ThreadLocalRandom.current()).orElseThrow(() -> new RuntimeException("HMI: Error: Tag \"" + recipe[offset-1 /* effectively final, shut it, java */].toString() + "\" does not exist in the registry!")).value();
                        ItemStack displayItem = new ItemStack(theHolyOneLiner);
                        items[j][offset] = displayItem;
                        if (recipe[i] != null && displayItem.getDamage() == -1) {
                            if (displayItem.hasSubtypes()) {
                                if (filter != null && displayItem.itemId == filter.itemId) {
                                    items[j][offset] = new ItemStack(displayItem.getItem(), 0, filter.getDamage());
                                } else {
                                    items[j][offset] = new ItemStack(displayItem.getItem());
                                }
                            } else if (filter != null && displayItem.itemId == filter.itemId) {
                                items[j][offset] = new ItemStack(displayItem.getItem(), 0, filter.getDamage());
                            }
                        }
                    }
                }
                items[j][0] = (ItemStack) recipeObj[0];
                ItemStack[] fuels = FuelRegistry.getFuelsView().keySet().toArray(ItemStack[]::new);
                items[j][2] = fuels[RANDOM.nextInt(fuels.length)];
            }

            if (items[j][0] == null && recipesOnThisPage > j) {
                recipesOnThisPage = j;
                redrawSlots = true;
                break;
            } else if (items[j][0] != null && recipesOnThisPage == j) {
                recipesOnThisPage = j + 1;
                redrawSlots = true;
            }
        }

        return items;
    }

    @Override
    public void updateRecipes(ItemStack filter, Boolean getUses) {
        recipes.clear();
        updateRecipesWithoutClear(filter, getUses);
    }

    @Override
    public Class<? extends HandledScreen> getGuiClass() {
        return FurnaceScreen.class;
    }

    public void updateRecipesWithoutClear(ItemStack filter, Boolean getUses) {
        lastIndex = 0;
        for (Object obj : recipesComplete.keySet()) {
            int dmg = 0;
            if (filter != null) dmg = filter.getDamage();

            ItemStack output = (ItemStack) (recipesComplete.get(obj));
            Object input = null;
            if (obj != null) {
                if (obj instanceof ItemStack) {
                    obj = new ItemStack[]{((ItemStack) obj).copy()};
                } else if (obj instanceof TagKey<?>) {
                    //noinspection unchecked
                    TagKey<Item> finalObj = (TagKey<Item>) obj;
                    Item theHolyOneLiner = ItemRegistry.INSTANCE.getOrCreateEntryList(finalObj).getRandom(ThreadLocalRandom.current()).orElseThrow(() -> new RuntimeException("HMI: Error: Tag \"" + finalObj.toString() + "\" does not exist in the registry!")).value();

                    obj = new ItemStack[]{new ItemStack(theHolyOneLiner)};
                } else if (obj instanceof TagKey<?>[]) {
                    ArrayList<ItemStack> coolStuff = new ArrayList<>();
                    //noinspection unchecked
                    for (TagKey<Item> entry : (TagKey<Item>[]) obj) {
                        Item theHolyOneLiner = ItemRegistry.INSTANCE.getOrCreateEntryList(entry).getRandom(ThreadLocalRandom.current()).orElseThrow(() -> new RuntimeException("HMI: Error: Tag \"" + entry.toString() + "\" does not exist in the registry!")).value();
                        coolStuff.add(new ItemStack(theHolyOneLiner));
                    }
                    obj = coolStuff.toArray(new ItemStack[]{});
                }
                if (obj instanceof Integer) {
                    if ((Integer) obj < Block.BLOCKS.length) {
                        if (Block.BLOCKS[(Integer) obj] == null) continue;
                        input = new ItemStack[]{new ItemStack(Block.BLOCKS[(Integer) obj], 1, dmg)};
                    } else {
                        if ((Integer) obj < Item.ITEMS.length) {
                            input = new ItemStack[]{new ItemStack(Item.ITEMS[(Integer) obj], 1, dmg)};
                        } else if (damagedFurnaceInput && (Integer) obj - (output.getDamage() << 16) < Block.BLOCKS.length) {
                            if (Block.BLOCKS[(Integer) obj - (output.getDamage() << 16)] == null) continue;
                            input = new ItemStack[]{new ItemStack(Block.BLOCKS[(Integer) obj - (output.getDamage() << 16)], 1, output.getDamage())};
                        } else continue;
                    }
                } else if (obj instanceof ItemStack[]) {
                    input = obj;
                } else throw new ClassCastException("Invalid recipe item type " + obj.getClass().getName() + "!");
            }
            if (input != null && (filter == null || (getUses && Arrays.stream(((ItemStack[]) input)).allMatch((inp) -> inp.itemId == filter.itemId)) || (!getUses && output.itemId == filter.itemId && (output.getDamage() == filter.getDamage() || output.getDamage() < 0 || !output.hasSubtypes())))) {
                recipes.add(new Object[]{output, input});
            } else if (filter == null) throw new ClassCastException("Invalid recipe item type " + input.getClass().getName() + "!");
        }
        size = recipes.size();
        super.updateRecipes(filter, getUses);
        size = recipes.size();
    }

    @Override
    public ItemStack getTabItem() {
        return new ItemStack(tabBlock, 1, metadata);
    }
}
