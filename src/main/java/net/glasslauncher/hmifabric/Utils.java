package net.glasslauncher.hmifabric;

import net.fabricmc.loader.api.FabricLoader;
import net.glasslauncher.hmifabric.event.HMIItemListRefreshEvent;
import net.glasslauncher.hmifabric.mixin.access.ContainerBaseAccessor;
import net.glasslauncher.hmifabric.tabs.Tab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.platform.Lighting;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.screen.slot.Slot;
import net.modificationstation.stationapi.api.StationAPI;
import net.modificationstation.stationapi.api.client.event.gui.screen.container.TooltipRenderEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class Utils {
    private static ArrayList<ItemStack> allItems;
    public static ItemRenderer itemRenderer = new ItemRenderer();
    public static Random rand = new Random();
    public static DrawContext gui = new DrawContext();

    @SuppressWarnings("deprecation")
    public static Minecraft getMC() {
        return (Minecraft) FabricLoader.getInstance().getGameInstance();
    }

    private static final List<String> loadedResources = new ArrayList<>();
    private static final List<String> missingResources = new ArrayList<>();
    private static final String resourcesFolder = "/hmi/resources/";

    //Returns the translated name of an itemstack with its ID if config allows and ID is wanted
    public static String getNiceItemName(ItemStack item, boolean withID) {
        String s = TranslationStorage.getInstance().getClientTranslation(item.getTranslationKey());
        if (s == null || s.isEmpty()) {
            s = item.getTranslationKey();
            if (s == null) s = "null";
        }
        if (Config.config.showItemIDs && withID) {
            s += " " + item.itemId;
            if (item.hasSubtypes()) s += ":" + item.getDamage();
        }
        return s;
    }

    //Returns the translated name of an itemstack with its ID if config allows
    public static String getNiceItemName(ItemStack item) {
        return getNiceItemName(item, true);
    }

    //Returns the item that the user is hovering in their inventory
    public static ItemStack hoveredItem(HandledScreen gui, int posX, int posY) {
        try {
            Slot slotAtPosition = ((ContainerBaseAccessor) gui).invokeGetSlot(posX, posY);
            if (slotAtPosition != null) return slotAtPosition.getStack();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //Returns the list of all blocks and items (that I can find)
    public static ArrayList<ItemStack> itemList() {
        if (allItems == null) {
            ;
            allItems = new ArrayList<>();

            Item[] mcItemsList = Item.ITEMS;
            for (Item item : mcItemsList) {
                if (item == null) {
                    continue;
                }
                HashSet<String> currentItemNames = new HashSet<>();
                for (int dmg = 0; ; dmg++) {
                    ItemStack itemstack = new ItemStack(item, 1, dmg);
                    for (ItemStack hiddenItem : GuiOverlay.hiddenItems) {
                        if (itemstack.isItemEqual(hiddenItem)) {
                            itemstack = hiddenItem;
                            break;
                        }
                    }
                    try {
                        int l = item.getTextureId(itemstack);
                        String s = TranslationStorage.getInstance().getClientTranslation(itemstack.getTranslationKey());
                        if (s.isEmpty()) s = itemstack.getTranslationKey() + "@" + l;
                        if (dmg >= 4 && (s.contains(String.valueOf(dmg)) || s.contains(String.valueOf(dmg + 1)) || s.contains(String.valueOf(dmg - 1)))) {
                            break;
                        }
                        s = s + "@" + l;
                        //System.out.println(s);
                        if (!currentItemNames.contains(s)) {
                            allItems.add(itemstack);
                            currentItemNames.add(s);
                            continue;
                        } else {
                            break;
                        }
                    } catch (NullPointerException | IndexOutOfBoundsException ignored) {
                    }
                    break;
                }
            }

            //This field should *only* ever contain things that implement Recipe, so this cast is fine.
            //noinspection unchecked
            List<CraftingRecipe> recipes = (List<CraftingRecipe>) CraftingRecipeManager.getInstance().getRecipes();
            for (CraftingRecipe recipe : recipes) {
                if (recipe != null && recipe.getOutput() != null && recipe.getOutput().getItem() != null) {
                    ItemStack itemstack = new ItemStack(recipe.getOutput().getItem(), 1, recipe.getOutput().getDamage());
                    for (ItemStack hiddenItem : GuiOverlay.hiddenItems) {
                        if (itemstack.isItemEqual(hiddenItem)) {
                            itemstack = hiddenItem;
                            break;
                        }
                    }
                    if (!itemstack.hasSubtypes()) {
                        continue;
                    }
                    addItemInOrder(allItems, itemstack);
                }
            }
            FabricLoader.getInstance().getEntrypoints(HowManyItems.MODID.id("item_list_refresh").toString(), HMIItemListRefreshEvent.class).forEach(hmiItemListRefreshEvent -> hmiItemListRefreshEvent.refreshItemList(allItems));

        }
        return allItems;
    }

    public static void addItemInOrder(ArrayList<ItemStack> itemList, ItemStack itemstack) {
        for (ItemStack item : itemList) {
            if (item.isItemEqual(itemstack)) {
                return;
            }
            if (item.itemId > itemstack.itemId || (item.itemId == itemstack.itemId && item.getDamage() > itemstack.getDamage())) {
                itemList.add(itemList.indexOf(item), itemstack);
                return;
            }
        }
    }

    //Returns the default list of items that are hidden in the overlay
    public static ArrayList<ItemStack> hiddenItems = new ArrayList<>();

    //Returns the number of enabled tabs
    public static int visibleTabSize() {
        int largestIndex = -1;
        for (Tab tab : HowManyItemsClient.getTabs()) {
            if (tab.index > largestIndex)
                largestIndex = tab.index;
        }
        return largestIndex + 1;
    }

    public static void drawScaledItem(ItemStack item, int x, int y, int length) {
        GL11.glPushMatrix();
        float scaleFactor = (float) length / 16;
        GL11.glScalef(scaleFactor, scaleFactor, 1);
        drawItemStack((int) (x / scaleFactor), (int) (y / scaleFactor), item, true);
        GL11.glPopMatrix();
    }

    public static void drawRect(int i, int j, int k, int l, int colour) {
        disableLighting();
        HowManyItemsClient.drawRect(i, j, k, l, colour);
    }

    public static void drawSlot(int x, int y, int colour) {
        drawRect(x, y, x + 18, y + 18, colour);
    }

    public static void drawArrow(int x, int y) {
        disableLighting();
        bindTexture();
        gui.drawTexture(x, y, 0, 63, 25, 25);
    }

    public static void drawSlot(int x, int y) {
        disableLighting();
        bindTexture();
        gui.drawTexture(x, y, 25, 63, 25, 25);
    }

    public static void drawTooltip(String s, int x, int y) {
        //stores tooltip info, to be drawn after overlay
        tooltipText = s;
        tooltipX = x;
        tooltipY = y;
    }

    public static void setTooltipData(ItemStack st, PlayerInventory inv) {
        stack = st;
        inventory = inv;
    }

    public static void drawStoredToolTip() {
        if (tooltipText != null) {
            disableLighting();
            if (stack == null || inventory == null) {
                int k1 = tooltipX + 12;
                int i2 = tooltipY - 12;
                int j2 = getMC().textRenderer.getWidth(tooltipText);
                drawRect(k1 - 3, i2 - 3, k1 + j2 + 3, i2 + 8 + 3, 0xc0000000);
                getMC().textRenderer.drawWithShadow(tooltipText, k1, i2, -1);
            } else {
                StationAPI.EVENT_BUS.post(
                        TooltipRenderEvent.builder()
                                .itemStack(stack)
                                .textManager(getMC().textRenderer)
                                .inventory(inventory)
                                .mouseX(tooltipX).mouseY(tooltipY)
                                .delta(0)
                                .originalTooltip(tooltipText)
                                .build()
                );
            }
            postRender();
        }
        tooltipText = null;
        stack = null;
    }

    public static void disableLighting() {
        if (lighting != Boolean.FALSE) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDisable(2896 /*GL_LIGHTING*/);
            GL11.glDisable(2929 /*GL_DEPTH_TEST*/);
            lighting = false;
        }
    }

    private static void enableLighting() {
        if (lighting != Boolean.TRUE) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(2896 /*GL_LIGHTING*/);
            GL11.glEnable(2929 /*GL_DEPTH_TEST*/);
            lighting = true;
        }
    }

    public static void bindTexture(String texturePath) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        getMC().textureManager.bindTexture(getMC().textureManager.getTextureId(texturePath));
        localTextureBound = false;
    }

    public static void bindTexture() {
        if (!localTextureBound) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            getMC().textureManager.bindTexture(getMC().textureManager.getTextureId(getResource("/assets/hmifabric/textures/icons.png")));
            localTextureBound = true;
        }
    }

    public static void drawItemStack(int x, int y, ItemStack item, boolean drawOverlay) {
        localTextureBound = false;
        enableItemLighting();
        itemRenderer.renderGuiItem(getMC().textRenderer, getMC().textureManager, item, x, y);
        if (drawOverlay) {
            itemRenderer.renderGuiItemDecoration(getMC().textRenderer, getMC().textureManager, item, x, y);
        }
    }

    private static void enableItemLighting() {
        enableLighting();
        if (!itemLighting) {
            GL11.glEnable(32826 /*GL_RESCALE_NORMAL_EXT*/);
            GL11.glPushMatrix();
            GL11.glRotatef(120F, 1.0F, 0.0F, 0.0F);
            Lighting.turnOn();
            GL11.glPopMatrix();
            itemLighting = true;
        }
    }

    private static boolean itemLighting;
    private static boolean localTextureBound;
    public static Boolean lighting;
    private static String tooltipText;
    private static int tooltipX;
    private static int tooltipY;
    private static ItemStack stack;
    private static PlayerInventory inventory;

    public static void drawString(String s, int x, int y) {
        disableLighting();
        int k1 = (x) + 12;
        int i2 = y - 12;
        getMC().textRenderer.drawWithShadow(s, k1, i2, -1);
    }

    public static void preRender() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        lighting = null;
        itemLighting = false;
        localTextureBound = false;
    }

    public static void postRender() {
        lighting = null;
        Lighting.turnOff();
        enableLighting();
    }

    public static boolean classExists(String className) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    public static boolean nmsClassExists(String className) {
        return classExists(className) || classExists("net.minecraft.src." + className);
    }

    public static String getResource(String resource) {
        resourceExists(resource);
        return resource;
    }

    public static boolean resourceExists(String resource) {
        if (loadedResources.contains(resource)) {
            return true;
        }
        if (missingResources.contains(resource)) {
            return false;
        }
        if (Utils.class.getResource(resource) != null) {
            loadedResources.add(resource);
            return true;
        } else {
            missingResources.add(resource);
            String error = "Missing file " + new File("bin\\minecraft.jar").getAbsolutePath().replace("\\", "/") + resource;
            if (System.getProperty("java.class.path").toLowerCase().contains("eclipse")) {
                logError(new String[]{error, "Alternate Location " + HowManyItemsClient.class.getClassLoader().getResource("").getPath().replaceFirst("/*$", "") + resource});
            } else {
                logError(error);
            }
            return false;
        }
    }

    public static void logError(String... lines) {
        System.out.println("HMI ERROR: " + lines[0]);
        for (String message : lines) {
            if (Objects.equals(message, lines[0])) continue;
            System.out.println('\t' + message);
        }
    }

    public static Object getHandler(String path) {
        try {
            return Utils.class.getClassLoader().loadClass(Utils.class.getPackage().getName() + ".references." + path + ".ConcreteHandler").newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isKeyDown(KeyBinding keybind) {
        return isKeyDown(keybind.code);
    }

    public static boolean isKeyDown(int keyCode) {
        return Keyboard.isKeyDown(keyCode) && keyCodeValid(keyCode);
    }

    public static boolean keyEquals(int keyCode, KeyBinding keybind) {
        return keyCode == keybind.code && keybindValid(keybind);
    }

    public static boolean keybindValid(KeyBinding keybind) {
        return keyCodeValid(keybind.code);
    }

    public static boolean keyCodeValid(int keyCode) {
        return keyCode != Keyboard.KEY_NONE;
    }

    public static class EasyField<T> {

        private static final EasyField<Integer> modifiersField = new EasyField<>(Field.class, "modifiers");
        public final Field field;

        public EasyField(Object value, Class<?> target) {
            this(value, target, null);
        }

        public EasyField(Object value, Class<?> target, Object instance) {
            Field correctField = null;
            for (Field field : target.getDeclaredFields()) {
                try {
                    if (field.get(instance) == value) {
                        field.setAccessible(true);
                        correctField = field;
                        break;
                    }
                } catch (Exception e) {
                }
            }
            this.field = correctField;
            if (this.field == null)
                logError("Failed to locate field " + value.getClass().getSimpleName() + " in class " + target.getSimpleName());
        }

        public EasyField(Class<?> target, String... names) {
            for (Field field : target.getDeclaredFields()) {
                for (String name : names) {
                    if (field.getName().equals(name)) {
                        field.setAccessible(true);
                        this.field = field;
                        return;
                    }
                }
            }
            this.field = null;
            logError("Failed to locate field " + names[0] + " in class " + target.getSimpleName());
        }

        public boolean exists() {
            return field != null;
        }

        @SuppressWarnings("unchecked")
        public T get(Object instance) {
            try {
                return (T) field.get(instance);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public T get() {
            return this.get(null);
        }

        public void set(Object instance, T value) {
            try {
                field.set(instance, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void set(T value) {
            this.set(null, value);
        }

        public void removeFinalModifier() {
            modifiersField.set(field, field.getModifiers() & ~Modifier.FINAL);
        }

    }

    public static Object getPrivateValue(Class instanceclass, Object instance, int fieldindex)
            throws IllegalArgumentException, SecurityException, NoSuchFieldException {
        try {
            Field f = instanceclass.getDeclaredFields()[fieldindex];
            f.setAccessible(true);
            return f.get(instance);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isGuiOpen(Class gui) {
        if (gui == null) {
            return getMC().currentScreen == null;
        }
        if (getMC().currentScreen == null) {
            return false;
        } else {
            return gui.isInstance(getMC().currentScreen);
        }
    }
}
