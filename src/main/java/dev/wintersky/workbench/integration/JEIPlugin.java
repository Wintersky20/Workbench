package dev.wintersky.workbench.integration;

import dev.wintersky.workbench.WorkbenchMod;
import dev.wintersky.workbench.api.WorkbenchRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

@JeiPlugin
public class JEIPlugin implements IModPlugin
{
    private static final ResourceLocation ID = WorkbenchMod.location("jei_plugin");

    @Override
    public ResourceLocation getPluginUid()
    {
        return ID;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
    {
        registration.addRecipeCatalyst(new ItemStack(WorkbenchMod.WORKBENCH_BLOCK.get()), WorkbenchCategory.UID);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry)
    {
        registry.addRecipeCategories(new WorkbenchCategory(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration)
    {
        ClientLevel world = Objects.requireNonNull(Minecraft.getInstance().level);
        registration.addRecipes(WorkbenchRecipe.getAllRecipes(world), WorkbenchCategory.UID);
    }
}