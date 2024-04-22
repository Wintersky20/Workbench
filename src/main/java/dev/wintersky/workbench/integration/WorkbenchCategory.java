package dev.wintersky.workbench.integration;

import dev.wintersky.workbench.WorkbenchMod;
import dev.wintersky.workbench.api.WorkbenchRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WorkbenchCategory implements IRecipeCategory<WorkbenchRecipe>
{
    private static final ResourceLocation GUI_TEXTURE_LOCATION = WorkbenchMod.location("textures/gui/workbench.png");
    public static final ResourceLocation UID = WorkbenchMod.location("drying");
    public static final RecipeType<WorkbenchRecipe> CRAFTING = new RecipeType<>(UID, WorkbenchRecipe.class);

    public static WorkbenchCategory INSTANCE;

    private final IDrawable background;
    private final IDrawable icon;

    public WorkbenchCategory(IGuiHelper guiHelper)
    {
        INSTANCE = this;
        background = guiHelper.createDrawable(GUI_TEXTURE_LOCATION, 6, 12, 159, 61);
        icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM, new ItemStack(WorkbenchMod.WORKBENCH_BLOCK.get()));
    }

    @SuppressWarnings("removal")
    @Deprecated
    @Nonnull
    @Override
    public ResourceLocation getUid()
    {
        return UID;
    }

    @SuppressWarnings("removal")
    @Deprecated
    @Override
    public Class<? extends WorkbenchRecipe> getRecipeClass()
    {
        return WorkbenchRecipe.class;
    }

    @Override
    public RecipeType<WorkbenchRecipe> getRecipeType()
    {
        return CRAFTING;
    }

    @Nonnull
    @Override
    public Component getTitle()
    {
        return new TranslatableComponent("jei.category.workbench.crafting");
    }

    @Nonnull
    @Override
    public IDrawable getBackground()
    {
        return background;
    }

    @Override
    public IDrawable getIcon()
    {
        return icon;
    }

    /**
     * Sets all the recipe's ingredients by filling out an instance of {@link IRecipeLayoutBuilder}.
     * This is used by JEI for lookups, to figure out what ingredients are inputs and outputs for a recipe.
     *
     * @since 9.4.0
     */
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WorkbenchRecipe recipe, IFocusGroup focuses)
    {

        var tool = Arrays.stream(recipe.getTool().getItems()).toList();
        var second_tool = Arrays.stream(recipe.getSecondTool().getItems()).toList();
        var inputs = recipe.getMaterials();

        List<List<ItemStack>> inputLists = new ArrayList<>();
        for (WorkbenchRecipe.Material material : inputs)
        {
            ItemStack[] stacks = material.ingredient.getItems();
            List<ItemStack> expandedInput = Arrays.stream(stacks).map(stack -> {
                ItemStack copy = stack.copy();
                copy.setCount(material.count);
                return copy;
            }).collect(Collectors.toList());
            inputLists.add(expandedInput);
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, slotX[0], slotY[0])
                .addItemStacks(tool)
                .setSlotName("tool");
        builder.addSlot(RecipeIngredientRole.CATALYST, slotX[1], slotY[1])
                .addItemStacks(second_tool)
                .setSlotName("second_tool");

        for (int i = 0; i < 4; i++)
        {
            builder.addSlot(RecipeIngredientRole.INPUT, slotX[2 + i], slotY[2 + i])
                    .addItemStacks((i < inputLists.size()) ? inputLists.get(i) : List.of());
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, slotX[6], slotY[6])
                .addItemStack(recipe.getResultItem());
    }

    private static final int[] slotX = {
            8 - 6,
            30 - 6,
            10 - 6,
            28 - 6,
            10 - 6,
            28 - 6,
            143 - 6
    };

    private static final int[] slotY = {
            15 - 12,
            15 - 12,
            35 - 12,
            35 - 12,
            53 - 12,
            53 - 12,
            33 - 12
    };
}