package net.glasslauncher.hmifabric.mixin.access;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShapedRecipe.class)
public interface ShapedRecipeAccessor {
    @Accessor("width")
    int getWidth();
    @Accessor("height")
    int getHeight();
    @Accessor("input")
    ItemStack[] getInput();
}
