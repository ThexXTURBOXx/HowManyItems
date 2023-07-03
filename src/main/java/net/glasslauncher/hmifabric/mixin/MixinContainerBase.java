package net.glasslauncher.hmifabric.mixin;

import net.glasslauncher.hmifabric.Config;
import net.glasslauncher.hmifabric.GuiOverlay;
import net.glasslauncher.hmifabric.HowManyItemsClient;
import net.glasslauncher.hmifabric.KeyBindings;
import net.glasslauncher.hmifabric.Utils;
import net.minecraft.client.gui.screen.container.ContainerBase;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerBase.class)
public class MixinContainerBase {

    @Inject(method = "keyPressed(CI)V", at = @At(value = "HEAD"))
    private void keyPressed(char character, int key, CallbackInfo ci) {
        if (Keyboard.getEventKey() == KeyBindings.toggleOverlay.key && Utils.keybindValid(KeyBindings.toggleOverlay)) {
            if (Utils.isGuiOpen(ContainerBase.class) && !GuiOverlay.searchBoxFocused()) {
                Config.config.overlayEnabled = !Config.config.overlayEnabled;
                if (HowManyItemsClient.thisMod.overlay != null) HowManyItemsClient.thisMod.overlay.toggle();
            }
        }
    }
}
