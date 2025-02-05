package net.glasslauncher.hmifabric.mixin.access;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShapelessRecipe.class)
public interface ShapelessRecipeAccessor {
    @Accessor("input")
    List<ItemStack> getInput();
}
