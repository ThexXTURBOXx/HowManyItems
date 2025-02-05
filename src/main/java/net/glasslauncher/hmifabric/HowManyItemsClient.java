package net.glasslauncher.hmifabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.glasslauncher.hmifabric.event.HMITabRegistryEvent;
import net.glasslauncher.hmifabric.mixin.access.DrawableHelperAccessor;
import net.glasslauncher.hmifabric.tabs.Tab;
import net.glasslauncher.hmifabric.tabs.TabCrafting;
import net.glasslauncher.hmifabric.tabs.TabRegistry;
import net.glasslauncher.hmifabric.tabs.TabSmelting;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.ScreenScaler;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.client.event.network.MultiplayerLogoutEvent;
import net.modificationstation.stationapi.api.client.event.option.KeyBindingRegisterEvent;
import net.modificationstation.stationapi.api.event.registry.MessageListenerRegistryEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.Entrypoint;
import net.modificationstation.stationapi.api.registry.Registry;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.util.Namespace;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.logging.*;

import static net.glasslauncher.hmifabric.Utils.hiddenItems;

public class HowManyItemsClient {

    public static Logger logger = Logger.getLogger(HowManyItemsClient.class.getName());

    @Environment(EnvType.CLIENT)
    public GuiOverlay overlay;

    @Entrypoint.Instance
    public static HowManyItemsClient thisMod;

    @EventListener
    public void registerKeyBindings(KeyBindingRegisterEvent event) {
        event.keyBindings.add(KeyBindings.toggleOverlay);
    }

    public static void addGuiToBlock(Class<? extends HandledScreen> gui, ItemStack item) {
        TabUtils.putItemGui(gui, item);
    }

    public static void addWorkBenchGui(Class<? extends HandledScreen> gui) {
        TabUtils.addWorkBenchGui(gui);
    }

    public static void addEquivalentWorkbench(ItemStack item) {
        TabUtils.addEquivalentWorkbench(item);
    }

    public static void addEquivalentFurnace(ItemStack item) {
        TabUtils.addEquivalentFurnace(item);
    }

    public static void onSettingChanged() {
        if (thisMod.overlay != null) thisMod.overlay.init();
    }

    public void onTickInGUI(Minecraft mc, Screen guiscreen) {
        if (guiscreen instanceof HandledScreen) {
            HandledScreen screen = (HandledScreen) guiscreen;
            if (Config.config.overlayEnabled) {
                if (GuiOverlay.screen != screen || overlay == null || screen.width != overlay.width || screen.height != overlay.height) {
                    overlay = new GuiOverlay(screen);
                }
                overlay.onTick();
            }
            Utils.drawStoredToolTip();
            if (Utils.isKeyDown(KeyBindings.pushRecipe) || Utils.isKeyDown(KeyBindings.pushUses)) {
                if (!keyHeldLastTick) {
                    boolean getUses = Utils.isKeyDown(KeyBindings.pushUses);
                    ItemStack newFilter = null;

                    ScreenScaler scaledresolution = new ScreenScaler(mc.options, mc.displayWidth, mc.displayHeight);
                    int i = scaledresolution.getScaledWidth();
                    int j = scaledresolution.getScaledHeight();
                    int posX = (Mouse.getEventX() * i) / mc.displayWidth;
                    int posY = j - (Mouse.getEventY() * j) / mc.displayHeight - 1;
                    newFilter = Utils.hoveredItem((HandledScreen) guiscreen, posX, posY);
                    if (newFilter == null) {
                        newFilter = GuiOverlay.hoverItem;
                    }
                    if (newFilter == null) {
                        if (guiscreen instanceof GuiRecipeViewer)
                            newFilter = ((GuiRecipeViewer) guiscreen).getHoverItem();
                    }
                    if (newFilter != null) {
                        pushRecipe(guiscreen, newFilter, getUses);
                    } else {
                        if (Config.config.overlayEnabled && guiscreen == GuiOverlay.screen && !GuiOverlay.searchBoxFocused() && Config.config.fastSearch) {
                            GuiOverlay.focusSearchBox();
                        }
                    }
                }
            } else if (Utils.isKeyDown(KeyBindings.prevRecipe)) {
                if (!keyHeldLastTick) {
                    if (guiscreen instanceof GuiRecipeViewer && !GuiOverlay.searchBoxFocused()) {
                        ((GuiRecipeViewer) guiscreen).pop();
                    } else {
                        if (Config.config.overlayEnabled && guiscreen == GuiOverlay.screen && !GuiOverlay.searchBoxFocused() && Config.config.fastSearch)
                            if (!GuiOverlay.emptySearchBox()) GuiOverlay.focusSearchBox();
                    }
                }
            } else if (KeyBindings.clearSearchBox.code == KeyBindings.focusSearchBox.code
                    && Utils.isKeyDown(KeyBindings.clearSearchBox)) {

                if (System.currentTimeMillis() > focusCooldown) {
                    focusCooldown = System.currentTimeMillis() + 800L;
                    if (!GuiOverlay.searchBoxFocused())
                        GuiOverlay.clearSearchBox();
                    GuiOverlay.focusSearchBox();
                }
            } else if (Utils.isKeyDown(KeyBindings.clearSearchBox)) {
                GuiOverlay.clearSearchBox();
            } else if (Utils.isKeyDown(KeyBindings.focusSearchBox)) {
                if (System.currentTimeMillis() > focusCooldown) {
                    focusCooldown = System.currentTimeMillis() + 800L;
                    GuiOverlay.focusSearchBox();
                }
            } else if (Utils.isKeyDown(KeyBindings.allRecipes)) {
                pushRecipe(guiscreen, null, false);
            } else {
                keyHeldLastTick = false;
            }
            if (Utils.isKeyDown(KeyBindings.pushRecipe) || Utils.isKeyDown(KeyBindings.pushUses) || Utils.isKeyDown(KeyBindings.prevRecipe)) {
                keyHeldLastTick = true;
            }

        }
    }

    public void onTickInGame(Minecraft minecraft) {
        if (minecraft.currentScreen == null && Utils.isKeyDown(KeyBindings.allRecipes) && !keyHeldLastTick) {
            keyHeldLastTick = true;
            pushRecipe(null, null, false);
        }
    }

    public static boolean keyHeldLastTick = false;
    private static long focusCooldown = 0L;

    public static void pushRecipe(Screen gui, ItemStack item, boolean getUses) {
        if (Utils.getMC().player.inventory.getCursorStack() == null) {
            if (gui instanceof GuiRecipeViewer) {
                ((GuiRecipeViewer) gui).push(item, getUses);
            } else if (!GuiOverlay.searchBoxFocused() && getTabs().size() > 0) {
                GuiRecipeViewer newgui = new GuiRecipeViewer(item, getUses, gui);
                Utils.getMC().currentScreen = newgui;
                ScreenScaler scaledresolution = new ScreenScaler(Utils.getMC().options, Utils.getMC().displayWidth, Utils.getMC().displayHeight);
                int i = scaledresolution.getScaledWidth();
                int j = scaledresolution.getScaledHeight();
                newgui.init(Utils.getMC(), i, j);
                Utils.getMC().skipGameRender = false;
            }
        }
    }

    public static void pushTabBlock(Screen gui, ItemStack item) {
        if (gui instanceof GuiRecipeViewer) {
            ((GuiRecipeViewer) gui).pushTabBlock(item);
        } else if (!GuiOverlay.searchBoxFocused() && getTabs().size() > 0) {
            Utils.getMC().lockMouse();
            GuiRecipeViewer newgui = new GuiRecipeViewer(item, gui);
            Utils.getMC().currentScreen = newgui;
            ScreenScaler scaledresolution = new ScreenScaler(Utils.getMC().options, Utils.getMC().displayWidth, Utils.getMC().displayHeight);
            int i = scaledresolution.getScaledWidth();
            int j = scaledresolution.getScaledHeight();
            newgui.init(Utils.getMC(), i, j);
            Utils.getMC().skipGameRender = false;
        }
    }

    public static void drawRect(int i, int j, int k, int l, int i1) {
        // This is not that slow. Its only getting the method with reflection that is slow.
        try {
            ((DrawableHelperAccessor) Utils.gui).invokeFill(i, j, k, l, i1);
        } catch (Exception e) {
            logger.severe("Something went very wrong rendering a GUI!");
            e.printStackTrace();
        }
    }

    public static List<Tab> getTabs() {
        return TabRegistry.INSTANCE.getEntrySet().stream().map(Map.Entry::getValue).toList();
    }

    public static void tabOrderChanged(boolean[] tabEnabled, Tab[] tabOrder) {
        Config.tabOrderChanged(tabEnabled, tabOrder);
        Config.orderTabs();
    }

    @EventListener
    public void registerMessageListeners(MessageListenerRegistryEvent messageListenerRegistry) {
        Registry.register(messageListenerRegistry.registry, Identifier.of("hmifabric:handshake"), (playerBase, message) -> Config.isHMIServer = message.booleans[0]);
    }

    @EventListener
    public void onLogout(MultiplayerLogoutEvent event) {
        Config.isHMIServer = false;
    }

    @EventListener
    public void registerTabs(HMITabRegistryEvent event) {
        hiddenItems.add(new ItemStack(Block.WATER));
        hiddenItems.add(new ItemStack(Block.LAVA));
        hiddenItems.add(new ItemStack(Block.BED));
        hiddenItems.add(new ItemStack(Block.GRASS));
        hiddenItems.add(new ItemStack(Block.DEAD_BUSH));
        hiddenItems.add(new ItemStack(Block.PISTON_HEAD));
        hiddenItems.add(new ItemStack(Block.MOVING_PISTON));
        hiddenItems.add(new ItemStack(Block.DOUBLE_SLAB));
        hiddenItems.add(new ItemStack(Block.REDSTONE_WIRE));
        hiddenItems.add(new ItemStack(Block.WHEAT));
        hiddenItems.add(new ItemStack(Block.FARMLAND));
        hiddenItems.add(new ItemStack(Block.LIT_FURNACE));
        hiddenItems.add(new ItemStack(Block.SIGN));
        hiddenItems.add(new ItemStack(Block.DOOR));
        hiddenItems.add(new ItemStack(Block.WALL_SIGN));
        hiddenItems.add(new ItemStack(Block.IRON_DOOR));
        hiddenItems.add(new ItemStack(Block.LIT_REDSTONE_ORE));
        hiddenItems.add(new ItemStack(Block.REDSTONE_TORCH));
        hiddenItems.add(new ItemStack(Block.SUGAR_CANE));
        hiddenItems.add(new ItemStack(Block.CAKE));
        hiddenItems.add(new ItemStack(Block.REPEATER));
        hiddenItems.add(new ItemStack(Block.POWERED_REPEATER));
        hiddenItems.add(new ItemStack(Block.LOCKED_CHEST));

        event.registry.register(Identifier.of(Namespace.MINECRAFT, "crafting"), new TabCrafting(HowManyItems.MODID), new ItemStack(Block.CRAFTING_TABLE));
        event.registry.register(Identifier.of(Namespace.MINECRAFT, "smelting"), new TabSmelting(HowManyItems.MODID), new ItemStack(Block.FURNACE));
        event.registry.addEquivalentCraftingStation(Identifier.of(Namespace.MINECRAFT, "smelting"), new ItemStack(Block.LIT_FURNACE));
    }

    @EventListener(phase = StationAPI.INTERNAL_PHASE, priority = ListenerPriority.HIGHEST)
    public void disableVanillaTooltips(TooltipRenderEvent e) {
        if (e.container != null) {
            e.setCanceled(true);
        }
    }
}
