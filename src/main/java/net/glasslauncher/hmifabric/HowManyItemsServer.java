package net.glasslauncher.hmifabric;

import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.modificationstation.stationapi.api.event.registry.MessageListenerRegistryEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.Entrypoint;
import net.modificationstation.stationapi.api.network.ModdedPacketHandler;
import net.modificationstation.stationapi.api.network.packet.MessagePacket;
import net.modificationstation.stationapi.api.network.packet.PacketHelper;
import net.modificationstation.stationapi.api.registry.Registry;
import net.modificationstation.stationapi.api.server.event.network.PlayerLoginEvent;
import net.modificationstation.stationapi.api.util.Formatting;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.util.Namespace;
import net.modificationstation.stationapi.api.util.Null;

public class HowManyItemsServer {

    @Entrypoint.Namespace
    public static final Namespace MODID = Null.get();

    @EventListener
    public void handleLogin(PlayerLoginEvent event) {
        if (((ModdedPacketHandler) event.player.networkHandler).isModded()) {
            MessagePacket customData = new MessagePacket(Identifier.of("hmifabric:handshake"));
            customData.booleans = new boolean[]{true};
            PacketHelper.sendTo(((MinecraftServer) FabricLoader.getInstance().getGameInstance()).playerManager.getPlayer(event.loginHelloPacket.username), customData);
        }
    }

    @EventListener
    public void registerMessageListeners(MessageListenerRegistryEvent event) {
        Registry.register(event.registry, Identifier.of(MODID, "giveItem"), HowManyItemsServer::handleGivePacket);
        Registry.register(event.registry, Identifier.of(MODID, "heal"), HowManyItemsServer::handleHealPacket);
    }

    public static void handleGivePacket(PlayerEntity player, MessagePacket packet) {
        if (((MinecraftServer) FabricLoader.getInstance().getGameInstance()).playerManager.isOperator(player.name)) {
            Object[] objects = packet.deserializeObjects();
            if(objects == null || objects.length < 1) {
                player.sendMessage(Formatting.RED + "No items found to spawn?");
                return;
            }
            ItemStack itemInstance = (ItemStack) objects[0]; // Is this stupid? I have no idea.
            ItemEntity itemEntity = new ItemEntity(player.world, player.x, player.y, player.z, itemInstance);
            player.world.spawnEntity(itemEntity);
            player.sendMessage(Formatting.GRAY + "Spawned some " + itemInstance.getItem().getTranslatedName() + "...");
        }
        else if (!((MinecraftServer) FabricLoader.getInstance().getGameInstance()).playerManager.isOperator(player.name)) {
            player.sendMessage(Formatting.RED + "You need to be opped in order to give yourself items!");
        }
    }

    public static void handleHealPacket(PlayerEntity player, MessagePacket packet) {
        if (((MinecraftServer) FabricLoader.getInstance().getGameInstance()).playerManager.isOperator(player.name)) {
            player.heal(Integer.MAX_VALUE/2); // High to allow mods that mess with player health to be supported.
        }
    }
}
