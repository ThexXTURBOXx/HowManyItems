package net.glasslauncher.hmifabric;

import net.glasslauncher.hmifabric.tabs.Tab;
import net.glasslauncher.hmifabric.tabs.TabRegistry;
import net.glasslauncher.mods.gcapi3.api.ConfigEntry;
import net.glasslauncher.mods.gcapi3.api.ConfigRoot;
import net.glasslauncher.mods.gcapi3.api.GCAPI;
import net.glasslauncher.mods.gcapi3.impl.ConfigFactories;

import java.lang.reflect.*;
import java.util.*;
import net.glasslauncher.mods.gcapi3.impl.GlassYamlFile;

public class Config {

    public static void orderTabs() {
        ArrayList<Tab> orderedTabs = new ArrayList<>();
        for (int i = 0; i < TabRegistry.INSTANCE.tabOrder.size(); i++) {
            Tab tab = TabRegistry.INSTANCE.tabOrder.get(i);
            while (orderedTabs.size() < tab.index + 1)
                orderedTabs.add(null);
            if (tab.index >= 0) {
                orderedTabs.set(tab.index, tab);
            }
        }
        while (orderedTabs.remove(null)) {
        }
        for (int i = 0; i < orderedTabs.size(); i++) {
            orderedTabs.get(i).index = i;
        }
        for (int i = 0; i < TabRegistry.INSTANCE.tabOrder.size(); i++) {
            Tab tab = TabRegistry.INSTANCE.tabOrder.get(i);
            if (tab.index == -2) {
                tab.index = orderedTabs.size();
                orderedTabs.add(tab);
            } else if (tab.index < 0) {
                tab.index = -1;
            }
        }
        //writeConfig();
        TabRegistry.INSTANCE.tabOrder = orderedTabs;
    }

    public static void tabOrderChanged(boolean[] tabEnabled, Tab[] tabOrder) {
        for (int i = 0; i < TabRegistry.INSTANCE.tabOrder.size(); i++) {
            Tab tab = TabRegistry.INSTANCE.tabOrder.get(i);
            for (int j = 0; j < tabOrder.length; j++) {
                if (tab.equals(tabOrder[j])) {
                    tab.index = j;
                    if (!tabEnabled[j]) tab.index = -1;
                }
            }
        }
        //writeConfig();
    }

    public static void writeConfig() {
        try {
            GlassYamlFile cfg = new GlassYamlFile();
            for (Field field : ConfigFields.class.getFields()) {
                cfg.set(field.getName(), ConfigFactories.saveFactories.get(field.getType()).apply(field.get(config)));
            }
            cfg.set("forceNotMultiplayer", true);
            GCAPI.reloadConfig("hmifabric:config", cfg);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @ConfigRoot(value = "config", visibleName = "HMI Config")
    public static final ConfigFields config = new ConfigFields();

    public static boolean isHMIServer = false;

    public static class ConfigFields {
        @ConfigEntry(name = "Overlay Enabled")
        public Boolean overlayEnabled = true;
        @ConfigEntry(name = "Cheats Enabled")
        public Boolean cheatsEnabled = false;
        @ConfigEntry(name = "Show Item IDs")
        public Boolean showItemIDs = false;
        @ConfigEntry(name = "Center Search Bar")
        public Boolean centredSearchBar = false;
        @ConfigEntry(name = "Fast Search")
        public Boolean fastSearch = false;
        @ConfigEntry(name = "Inverted Scrolling")
        public Boolean scrollInverted = false;

        @ConfigEntry(name = "Multiplayer Give Command", multiplayerSynced = true)
        public String mpGiveCommand = "/give {0} {1} {2}";
        @ConfigEntry(name = "Multiplayer Heal Command", multiplayerSynced = true)
        public String mpHealCommand = "";
        @ConfigEntry(name = "Multiplayer Time Day Command", multiplayerSynced = true)
        public String mpTimeDayCommand = "/time set 0";
        @ConfigEntry(name = "Multiplayer Time Night Command", multiplayerSynced = true)
        public String mpTimeNightCommand = "/time set 13000";
        @ConfigEntry(name = "Multiplayer Rain On Command", multiplayerSynced = true)
        public String mpRainONCommand = "";
        @ConfigEntry(name = "Multiplayer Rain Off Command", multiplayerSynced = true)
        public String mpRainOFFCommand = "";

        @ConfigEntry(name = "Draggable Recipe Viewer")
        public Boolean recipeViewerDraggableGui = false;
        @ConfigEntry(name = "Show Null Name Items",
                     comment = "Shows items with null names. Can cause crashes with poorly made mods.")
        public Boolean hideNullNames = false;

        @ConfigEntry(name = "Developer Mode",
                     comment = "Enables some extra tooltips. Breaks relatively easily, but shouldn't cause crashes.")
        public Boolean devMode = false;

        @ConfigEntry(name = "Recipe Viewer GUI Width", maxLength = Integer.MAX_VALUE)
        public Integer recipeViewerGuiWidth = 251;
        @ConfigEntry(name = "Recipe Viewer GUI Height", maxLength = Integer.MAX_VALUE)
        public Integer recipeViewerGuiHeight = 134;
    }
}
