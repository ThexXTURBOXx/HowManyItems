package net.glasslauncher.hmifabric;

import net.glasslauncher.hmifabric.mixin.access.ContainerBaseAccessor;
import net.glasslauncher.hmifabric.tabs.Tab;
import net.glasslauncher.hmifabric.tabs.TabWithTexture;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionButtonWidget;
import net.minecraft.client.util.ScreenScaler;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;


public class GuiRecipeViewer extends HandledScreen {
    public GuiRecipeViewer(ItemStack itemstack, Boolean getUses, Screen parent) {
        super(container = new ContainerRecipeViewer(inv = new InventoryRecipeViewer(itemstack)));
        this.parent = parent;
        init2();
        push(itemstack, getUses);
    }

    public GuiRecipeViewer(ItemStack itemstack, Screen parent) {
        super(container = new ContainerRecipeViewer(inv = new InventoryRecipeViewer(itemstack)));
        this.parent = parent;
        init2();
        pushTabBlock(itemstack);
    }

    public void init2() {
        if (Config.config.recipeViewerDraggableGui) {
            backgroundWidth = Config.config.recipeViewerGuiWidth;
            backgroundHeight = Config.config.recipeViewerGuiHeight;
        } else {
            if (parent instanceof HandledScreen) {
                try {
                    backgroundWidth = ((ContainerBaseAccessor) parent).getContainerWidth();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                backgroundHeight -= 80;
            } else {
                backgroundWidth = 254;
                backgroundHeight = 136;
            }

        }
        tabs = HowManyItemsClient.getTabs();
        newTab(tabs.get(0));
    }

    public void pushTabBlock(ItemStack itemstack) {
        if (itemstack == null) {
            return;
        }
        inv.filter.push(null);
        inv.newList = true;
        inv.prevTabs.push(inv.currentTab);
        inv.prevPages.push(inv.getPage() * inv.currentTab.recipesPerPage);
        inv.prevGetUses.push(true);

        for (Tab tab : HowManyItemsClient.getTabs()) {
            boolean tabMatchesBlock = false;
            for (ItemStack tabBlock : tab.equivalentCraftingStations) {
                if (tabBlock.isItemEqual(itemstack)) {
                    tabMatchesBlock = true;
                    tab.updateRecipes(null, false);
                    break;
                }
            }
            if (!tabMatchesBlock) {
                tab.size = 0;
            }
        }
        postPush();
    }

    public void push(ItemStack itemstack, Boolean getUses) {
        if (!inv.filter.isEmpty() && itemstack == null && inv.filter.peek() == null) {
            return;
        }
        if (inv.filter.isEmpty() || getUses != inv.prevGetUses.peek() ||
                (itemstack == null && inv.filter.peek() != null) || (itemstack != null && inv.filter.peek() == null) ||
                (itemstack.itemId != inv.filter.peek().itemId || (itemstack.getDamage() != inv.filter.peek().getDamage() && itemstack.hasSubtypes()))) {

            inv.newList = true;
            if (itemstack == null) {
                inv.filter.push(null);
            } else
                inv.filter.push(new ItemStack(itemstack.getItem(), 1, itemstack.getDamage()));
            inv.prevTabs.push(inv.currentTab);
            inv.prevPages.push(inv.getPage() * inv.currentTab.recipesPerPage);
            inv.prevGetUses.push(getUses);

            inv.newList = true;
            for (Tab tab : HowManyItemsClient.getTabs()) {
                tab.updateRecipes(inv.filter.peek(), getUses);
            }
            postPush();
        }

    }

    private void postPush() {
        if (inv.currentTab.size == 0) {
            for (Tab tab : HowManyItemsClient.getTabs()) {
                if (tab.size > 0) {
                    newTab(tab);
                    break;
                }
                if (HowManyItemsClient.getTabs().indexOf(tab) == HowManyItemsClient.getTabs().size() - 1) {
                    inv.filter.pop();
                    inv.prevTabs.pop();
                    inv.prevPages.pop();
                    inv.prevGetUses.pop();
                    if (inv.filter.isEmpty()) {
                        inv.newList = false;
                        return;
                    } else
                        for (Tab tab2 : HowManyItemsClient.getTabs()) {

                            tab2.updateRecipes(inv.filter.peek(), inv.prevGetUses.peek());
                        }
                    inv.index = inv.setIndex(inv.index);
                    initButtons();
                    return;
                }
            }

        }

        inv.index = inv.setIndex(0);
        initButtons();
    }

    public void pop() {
        inv.filter.pop();
        inv.prevGetUses.pop();
        if (inv.filter.isEmpty()) {
            inv.newList = false;
            displayParent();
            return;
        } else {
            for (Tab tab : HowManyItemsClient.getTabs()) {
                tab.updateRecipes(inv.filter.peek(), inv.prevGetUses.peek());
            }
            newTab(inv.prevTabs.pop());
            inv.newList = true;
            inv.index = inv.setIndex(inv.prevPages.pop());
            initButtons();
        }
    }

    //Change page with scroll wheel
    @Override
    public void onMouseEvent() {
        int i = Mouse.getEventDWheel();
        //TODO if(mouse in gui bounds) {
        if (!Config.config.scrollInverted) {
            if (i > 0) {
                inv.incIndex();
                initButtons();
            }
            if (i < 0) {
                inv.decIndex();
                initButtons();
            }
        } else {
            if (i > 0) {
                inv.decIndex();
                initButtons();
            }
            if (i < 0) {
                inv.incIndex();
                initButtons();
            }
        }
        //}
        super.onMouseEvent();
    }

    @Override
    protected void mouseReleased(int i, int j, int k) {
        if (dragging && k != -1) {
            dragging = false;
        }
        if (dragging) {
            int x = (width - backgroundWidth) / 2;
            int y = (height - backgroundHeight) / 2;
            if (backgroundWidth != i - x || backgroundHeight != j - y) {
                if (i - x > tabs.get(tabIndex).WIDTH + 2 * EDGE_SIZE) backgroundWidth = i - x;
                if (j - y > tabs.get(tabIndex).HEIGHT + 2 * EDGE_SIZE) backgroundHeight = j - y;
                tabs.get(tabIndex).redrawSlots = true;
            }
        }
    }

    private Boolean dragging = false;

    @Override
    protected void mouseClicked(int posX, int posY, int k) {
        super.mouseClicked(posX, posY, k);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        ItemStack item = Utils.hoveredItem(this, posX, posY);
        if (item != null && minecraft.player.inventory.getCursorStack() == null) {
            push(item, k == 1);
        } else {
            //Start dragging to change gui size
            if (Config.config.recipeViewerDraggableGui && (posX - backgroundWidth + 10 > x) && (posX - backgroundWidth - 4 < x)
                    && (posY - backgroundHeight + 10 > y) && (posY - backgroundHeight - 4 < y) && k == 0 && !dragging) {
                dragging = true;
            }
            //Change page with LMB or RMB
            else if ((posX > x) && (posX < x + backgroundWidth)
                    && (posY > y + 4) && (posY < y + backgroundHeight + 4)) {
                if (k == 0) {
                    inv.incIndex();
                    initButtons();
                }
                if (k == 1) {
                    inv.decIndex();
                    initButtons();
                }
            } else {
                //Change tab
                int tabCount = 0;
                for (int z = tabPage; z < tabs.size() && (tabCount + 1) * TAB_WIDTH < backgroundWidth; z++) {
                    if (tabs.get(z).size > 0) {
                        if ((posX - tabCount * TAB_WIDTH + 1 > x) && (posX - (tabCount + 1) * TAB_WIDTH < x)
                                && (posY + 21 > y) && (posY - 3 < y)
                                && k == 0 && tabIndex != z) {
                            newTab(tabs.get(z));
                            break;
                        }
                        tabCount++;
                    }
                }
            }
        }
    }

    public void newTab(Tab tab) {
        tabIndex = tabs.indexOf(tab);
        tab.redrawSlots = true;
        inv.initTab(tab);
        initButtons();
    }

    //Change page with arrow keys
    @Override
    protected void keyPressed(char c, int i) {
        if (i == Keyboard.KEY_RIGHT) {
            inv.incIndex();
            initButtons();
        }
        if (i == Keyboard.KEY_LEFT) {
            inv.decIndex();
            initButtons();
        }
        if (i == Keyboard.KEY_ESCAPE || i == minecraft.options.inventoryKey.code) {
            displayParent();
            if (i == minecraft.options.inventoryKey.code) minecraft.player.closeHandledScreen();
        } else
            super.keyPressed(c, i);
    }

    @Override
    public void init() {
        super.init();
        if (inv.filter.isEmpty()) displayParent();
        else initButtons();
    }

    public void initButtons() {
        buttons.clear();
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        buttons.add(new OptionButtonWidget(-1, x + backgroundWidth - 20, y - 43, 20, 20, ">"));
        buttons.add(new OptionButtonWidget(-2, x, y - 43, 20, 20, "<"));
        ((ButtonWidget) buttons.get(0)).visible = tabPageButtons;
        ((ButtonWidget) buttons.get(0)).active = tabPageButton1;

        ((ButtonWidget) buttons.get(1)).visible = tabPageButtons;
        ((ButtonWidget) buttons.get(1)).active = tabPageButton2;

        for (int k = 0; k < buttons.size(); k++) {
            ButtonWidget guibutton = (ButtonWidget) buttons.get(k);
            guibutton.render(minecraft, cursorPosX, cursorPosY);
        }

        if (tabs.get(tabIndex) instanceof TabWithTexture) {
            TabWithTexture tab = (TabWithTexture) tabs.get(tabIndex);
            int gapX = (backgroundWidth - 2 * EDGE_SIZE) % (tab.WIDTH + tab.MIN_PADDING_X);
            int noX = (backgroundWidth - 2 * EDGE_SIZE) / (tab.WIDTH + tab.MIN_PADDING_X);
            if (noX == 0) noX++;

            int gapY = (backgroundHeight - 2 * EDGE_SIZE) % (tab.HEIGHT + tab.MIN_PADDING_Y);
            int noY = (backgroundHeight - 2 * EDGE_SIZE) / (tab.HEIGHT + tab.MIN_PADDING_Y);
            if (noY == 0) noY++;

            if (tab.size == 1) {
                noX = 1;
                noY = 1;
            }

            int i = 0;
            for (int l1 = 0; l1 < noX; l1++) {
                for (int i2 = 0; i2 < noY; i2++) {
                    if (tab.size > 0 && inv.items != null && i++ < tab.recipesOnThisPage && inv.items.length > i - 1 && tab.drawSetupRecipeButton(parent, inv.items[i - 1])) {
                        int posX = EDGE_SIZE + gapX / 4 + l1 * (backgroundWidth - gapX / 2) / noX;
                        int posY = EDGE_SIZE + gapY / 4 + i2 * (backgroundHeight - gapY / 2) / noY;
                        if (noX == 1) posX = (backgroundWidth - tab.WIDTH) / 2;
                        if (noY == 1) posY = (backgroundHeight - tab.HEIGHT) / 2;
                        GuiButtonHMI button = new GuiButtonHMI(i, x + posX + tab.BUTTON_POS_X, y + posY + tab.BUTTON_POS_Y, BUTTON_WIDTH, BUTTON_HEIGHT, "+");
                        Boolean[] itemsInInv = tab.itemsInInventory(parent, inv.items[i - 1]);
                        for (int qq = 0; qq < itemsInInv.length; qq++) {
                            if (!itemsInInv[qq]) {
                                button.active = false;
                                break;
                            }
                        }
                        buttons.add(button);
                    }
                }
            }
        }
    }

    private final int BUTTON_WIDTH = 12;
    private final int BUTTON_HEIGHT = 12;

    @Override
    protected void buttonClicked(ButtonWidget guibutton) {
        super.buttonClicked(guibutton);
        if (guibutton.id == -1) {
            tabPage += backgroundWidth / TAB_WIDTH;
            if (tabPage >= tabs.size()) tabPage -= backgroundWidth / TAB_WIDTH;
        } else if (guibutton.id == -2) {
            tabPage -= backgroundWidth / TAB_WIDTH;
            if (tabPage < 0) tabPage = 0;
        } else {
            if (guibutton.id - 1 < inv.items.length && tabs.get(tabIndex) instanceof TabWithTexture) {
                displayParent();
                ((TabWithTexture) tabs.get(tabIndex)).setupRecipe(parent, inv.items[guibutton.id - 1]);
            }
        }

    }

    public void displayParent() {
        //if (parent instanceof GuiInventory) {
        minecraft = Utils.getMC();
        minecraft.player.currentScreenHandler = minecraft.player.playerScreenHandler;
        //}
        this.removed();
        if (parent != null) {
            minecraft.currentScreen = parent;
            ScreenScaler scaledresolution = new ScreenScaler(minecraft.options, minecraft.displayWidth, minecraft.displayHeight);
            int i = scaledresolution.getScaledWidth();
            int j = scaledresolution.getScaledHeight();
            minecraft.unlockMouse();
            parent.init(minecraft, i, j);
        } else {
            minecraft.setScreen(parent);
        }
    }

    private int cursorPosX;
    private int cursorPosY;
    private int tabPage = 0;

    @Override
    public void render(int i, int j, float f) {
        cursorPosX = i;
        cursorPosY = j;
        super.render(i, j, f);
    }

    @Override
    protected void drawForeground() {
        if ((double) inv.currentTab.size / inv.currentTab.recipesPerPage > 1) {
            String s = inv.getName();
            textRenderer.drawWithShadow(s, backgroundWidth - 4 - textRenderer.getWidth(s), 10, 0x404040);
        }
        int tabCount = 0;
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        for (int z = tabPage; z < tabs.size(); z++) {
            if (tabs.get(z).size > 0 && (tabCount + 1) * TAB_WIDTH < backgroundWidth) {
                if ((cursorPosX > x + tabCount * TAB_WIDTH + 6) && (cursorPosX < x + (tabCount + 1) * TAB_WIDTH - 6)
                        && (cursorPosY > y - 16) && (cursorPosY < y)) {

                    String s2 = (tabs.get(z).name());
                    if (!s2.isEmpty()) {
                        Utils.drawTooltip(s2, cursorPosX, cursorPosY);
                    }
                    break;
                }
                tabCount++;
            }
        }
        if (tabs.get(tabIndex) instanceof TabWithTexture) {
            TabWithTexture tab = (TabWithTexture) tabs.get(tabIndex);
            y += 3;
            int gapX = (backgroundWidth - 2 * EDGE_SIZE) % (tab.WIDTH + tab.MIN_PADDING_X);
            int noX = (backgroundWidth - 2 * EDGE_SIZE) / (tab.WIDTH + tab.MIN_PADDING_X);
            if (noX == 0) noX++;

            int gapY = (backgroundHeight - 2 * EDGE_SIZE) % (tab.HEIGHT + tab.MIN_PADDING_Y);
            int noY = (backgroundHeight - 2 * EDGE_SIZE) / (tab.HEIGHT + tab.MIN_PADDING_Y);
            if (noY == 0) noY++;

            if (tab.size == 1) {
                noX = 1;
                noY = 1;
            }

            int i = 0;
            for (int l1 = 0; l1 < noX; l1++) {
                for (int i2 = 0; i2 < noY; i2++) {
                    if (tab.size > 0 && i++ < tab.recipesOnThisPage) {
                        int posX = EDGE_SIZE + gapX / 4 + l1 * (backgroundWidth - gapX / 2) / noX;
                        int posY = EDGE_SIZE + gapY / 4 + i2 * (backgroundHeight - gapY / 2) / noY;
                        if (noX == 1) posX = (backgroundWidth - tab.WIDTH) / 2;
                        if (noY == 1) posY = (backgroundHeight - tab.HEIGHT) / 2;
                        if (tab.drawSetupRecipeButton(parent, inv.items[i - 1]))
                            if ((cursorPosX > x + posX + tab.BUTTON_POS_X - 1) && (cursorPosX < x + posX + tab.BUTTON_POS_X + BUTTON_WIDTH)
                                    && (cursorPosY > y + posY + tab.BUTTON_POS_Y - 3 - 1) && (cursorPosY < y + posY + tab.BUTTON_POS_Y - 3 + BUTTON_HEIGHT)) {
                                //System.out.println(inv.items[i - 1][0].getItemName());

                                Boolean[] itemsInInv = tab.itemsInInventory(parent, inv.items[i - 1]);

                                for (int qq = 0; qq < itemsInInv.length; qq++) {
                                    if (!itemsInInv[qq]) {
                                        drawTexture(
                                                posX + tab.slots[qq + 1][0], posY + tab.slots[qq + 1][1],
                                                posX + tab.slots[qq + 1][0] + 16, posY + tab.slots[qq + 1][1] + 16,
                                                0x80EC1C12, 0x80EC1C12);
                                    }
                                }

                            }
                    }
                }
            }
        }
        Utils.postRender();
        Utils.disableLighting();
    }

    public ItemStack getHoverItem() {
        int tabCount = 0;
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        for (int z = tabPage; z < tabs.size(); z++) {
            if (tabs.get(z).size > 0) {
                if ((cursorPosX > x + tabCount * TAB_WIDTH + 6) && (cursorPosX < x + (tabCount + 1) * TAB_WIDTH - 6)
                        && (cursorPosY > y - 16) && (cursorPosY < y)) {
                    return tabs.get(z).getTabItem();
                }
                tabCount++;
            }
        }
        return null;
    }

    private final int EDGE_SIZE = 4;
    private final int TAB_WIDTH = 27;
    private boolean tabPageButtons;
    private boolean tabPageButton1;
    private boolean tabPageButton2;

    @Override
    protected void drawBackground(float f) {
        Utils.preRender();
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2 + 3;


        //DRAW TABS
        int tabCount = 0;
        for (int z = tabPage; z < tabs.size(); z++) {
            if (tabs.get(z).size > 0) {
                if (z != tabIndex && (tabCount + 1) * TAB_WIDTH < backgroundWidth) {
                    Utils.bindTexture();
                    Utils.disableLighting();
                    drawTexture(x + tabCount * TAB_WIDTH, y - 25, 28, 113, 28, 28);
                    Utils.drawItemStack(x + 6 + tabCount * TAB_WIDTH, y - 18, tabs.get(z).getTabItem(), true);
                }
                tabCount++;
            }
        }
        //DRAW BACKGROUND
        Utils.bindTexture();
        Utils.disableLighting();
        for (int l1 = 0; l1 < backgroundWidth - EDGE_SIZE; l1 += 48) {
            for (int i2 = 0; i2 < backgroundHeight - EDGE_SIZE; i2 += 48) {
                drawTexture(x + EDGE_SIZE + l1, y + EDGE_SIZE + i2, 4, 145, backgroundWidth - l1 - EDGE_SIZE * 2, backgroundHeight - i2 - EDGE_SIZE * 2); //grey background
            }
        }
        //SETUP TAB PAGE BUTTONS
        if (buttons.size() >= 2) {
            if ((tabCount + tabPage) * TAB_WIDTH < backgroundWidth) {
                tabPage = 0;
                tabPageButtons = false;
            } else {
                tabPageButtons = true;
            }
            ((ButtonWidget) buttons.get(0)).visible = tabPageButtons;
            ((ButtonWidget) buttons.get(1)).visible = tabPageButtons;

            if (backgroundWidth / TAB_WIDTH >= tabCount) {
                tabPageButton1 = false;
            } else {
                tabPageButton1 = true;
            }
            if (tabPage == 0) {
                tabPageButton2 = false;
            } else {
                tabPageButton2 = true;
            }
            ((ButtonWidget) buttons.get(0)).active = tabPageButton1;
            ((ButtonWidget) buttons.get(1)).active = tabPageButton2;
        }

        //DRAW EDGES
        for (int l1 = 0; l1 < backgroundWidth - EDGE_SIZE; l1 += 48) {
            drawTexture(x + EDGE_SIZE + l1, y, 4, 141, backgroundWidth - l1 - EDGE_SIZE * 2, EDGE_SIZE); //top border
            drawTexture(x + EDGE_SIZE + l1, (y + backgroundHeight) - EDGE_SIZE, 4, 193, backgroundWidth - l1 - EDGE_SIZE * 2, EDGE_SIZE); //bottom border
        }

        for (int i2 = 0; i2 < backgroundHeight - EDGE_SIZE; i2 += 48) {
            drawTexture(x, y + EDGE_SIZE + i2, 0, 145, EDGE_SIZE, backgroundHeight - i2 - EDGE_SIZE * 2); //left border
            drawTexture((x + backgroundWidth) - EDGE_SIZE, y + EDGE_SIZE + i2, 52, 145, EDGE_SIZE, backgroundHeight - i2 - EDGE_SIZE * 2); //right border
        }

        drawTexture(x, y, 0, 141, EDGE_SIZE, EDGE_SIZE); //top left corner
        drawTexture(x + backgroundWidth - EDGE_SIZE, y, 52, 141, EDGE_SIZE, EDGE_SIZE); //top right corner
        drawTexture(x, y + backgroundHeight - EDGE_SIZE, 0, 193, EDGE_SIZE, EDGE_SIZE); //bottom left corner
        drawTexture(x + backgroundWidth - EDGE_SIZE, y + backgroundHeight - EDGE_SIZE, 52, 193, EDGE_SIZE, EDGE_SIZE); //bottom right corner

        //DRAW CURRENT TAB
        tabCount = 0;
        for (int z = tabPage; z < tabs.size(); z++) {
            if (tabs.get(z).size > 0) {
                if (z == tabIndex && (tabCount + 1) * TAB_WIDTH < backgroundWidth) {
                    Utils.bindTexture();
                    Utils.disableLighting();
                    drawTexture(x + tabCount * TAB_WIDTH, y - 25, 0, 113, 28, 28);
                    if (tabCount == 0) {
                        drawTexture(x, y, 0, 145, EDGE_SIZE, 5);
                    }
                    Utils.drawItemStack(x + 6 + tabCount * TAB_WIDTH, y - 18, tabs.get(z).getTabItem(), true);
                }
                tabCount++;
            }
        }

        Utils.bindTexture();
        Utils.disableLighting();
        //DRAW dragging indicator thing
        if (Config.config.recipeViewerDraggableGui) {
            drawTexture(x + backgroundWidth - 29, y + backgroundHeight - 29, 56, 169, 28, 28);
        }

        //DRAW RECIPE CONTAINER

        Tab tab = tabs.get(tabIndex);


        int gapX = (backgroundWidth - 2 * EDGE_SIZE) % (tab.WIDTH + tab.MIN_PADDING_X);
        int noX = (backgroundWidth - 2 * EDGE_SIZE) / (tab.WIDTH + tab.MIN_PADDING_X);
        if (noX == 0) noX++;

        int gapY = (backgroundHeight - 2 * EDGE_SIZE) % (tab.HEIGHT + tab.MIN_PADDING_Y);
        int noY = (backgroundHeight - 2 * EDGE_SIZE) / (tab.HEIGHT + tab.MIN_PADDING_Y);
        if (noY == 0) noY++;

        if (backgroundWidth < tab.WIDTH + 2 * EDGE_SIZE) backgroundWidth = tab.WIDTH + 2 * EDGE_SIZE;
        if (backgroundHeight < tab.HEIGHT + 2 * EDGE_SIZE) backgroundHeight = tab.HEIGHT + 2 * EDGE_SIZE;

        if (!tab.redrawSlots && tab.slots.length > 0 && container.slots.size() / tab.slots.length > tab.recipesOnThisPage) {
            //tab.recipesOnThisPage = container.slots.size() / tab.slots.length;
            tab.redrawSlots = true;
        }
        boolean redrawItems = false;
        if (tab.recipesPerPage != noX * noY) {
            tab.recipesPerPage = noX * noY;
            tab.recipesOnThisPage = tab.recipesPerPage;
            tab.redrawSlots = true;
            redrawItems = true;
        }
        if (tab.size == 1) {
            noX = 1;
            noY = 1;
        }

        if (tab.redrawSlots) {
            container.resetSlots();
        }
        int i = 0;
        for (int l1 = 0; l1 < noX; l1++) {
            for (int i2 = 0; i2 < noY; i2++) {
                if (tab.size > 0 && i++ < tab.recipesOnThisPage) {
                    int posX = EDGE_SIZE + gapX / 4 + l1 * (backgroundWidth - gapX / 2) / noX;
                    int posY = EDGE_SIZE + gapY / 4 + i2 * (backgroundHeight - gapY / 2) / noY;
                    if (noX == 1) posX = (backgroundWidth - tab.WIDTH) / 2;
                    if (noY == 1) posY = (backgroundHeight - tab.HEIGHT) / 2;
                    tab.draw(x + posX, y + posY, i - 1, cursorPosX, cursorPosY);
                    if (tab.redrawSlots) {
                        for (int q = 0; q < tab.slots.length; q++) {
                            container.addSlot(posX + tab.slots[q][0], posY + tab.slots[q][1]);
                        }
                    }

                }
            }
        }
        if (tab.redrawSlots) {
            inv.newList = true;
            if (redrawItems)
                inv.setIndex(inv.index);
            initButtons();
            tab.redrawSlots = false;
        }

        Utils.postRender();
    }

    @Override
    public void removed() {
        if (Config.config.recipeViewerGuiWidth != backgroundWidth || Config.config.recipeViewerGuiHeight != backgroundHeight) {
            Config.config.recipeViewerGuiWidth = backgroundWidth;
            Config.config.recipeViewerGuiHeight = backgroundHeight;
        }
    }


    private Screen parent;
    private int tabIndex;
    public static List<Tab> tabs;
    private static InventoryRecipeViewer inv;
    private static ContainerRecipeViewer container;


}
