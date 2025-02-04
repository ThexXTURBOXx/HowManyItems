package net.glasslauncher.hmifabric.mixin;

import net.glasslauncher.hmifabric.HowManyItemsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Unique
    private long clock = 0L;

    @Shadow
    private Minecraft client;

    @Inject(method = "onFrameUpdate", at = @At(value = "TAIL"))
    private void onTick(float delta, CallbackInfo ci) {
        long newClock = 0L;
        if (client.world != null && HowManyItemsClient.thisMod != null) {
            newClock = client.world.getTime();
            if (newClock != clock) {
                HowManyItemsClient.thisMod.onTickInGame(client);
            }
            if (client.currentScreen != null) {
                HowManyItemsClient.thisMod.onTickInGUI(client, client.currentScreen);
            }
        }
        clock = newClock;
    }

}
