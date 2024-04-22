package dev.wintersky.workbench.api;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.ToolAction;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class WorkbenchRecipeBuilder
{
    private String group;
    private Ingredient tool;
    private Ingredient second_tool;
    private final List<WorkbenchRecipe.Material> materials = Lists.newArrayList();
    private final Item result;
    private final int count;
    private CompoundTag tag;

    public static WorkbenchRecipeBuilder begin(Item result)
    {
        return begin(result, 1, null);
    }

    public static WorkbenchRecipeBuilder begin(Item result, int count)
    {
        return begin(result, count, null);
    }

    public static WorkbenchRecipeBuilder begin(Item result, CompoundTag tag)
    {
        return begin(result, 1, tag);
    }

    public static WorkbenchRecipeBuilder begin(Item result, int count, @Nullable CompoundTag tag)
    {
        return new WorkbenchRecipeBuilder(result, count, tag);
    }

    protected WorkbenchRecipeBuilder(Item result, int count, @Nullable CompoundTag tag)
    {
        this.result = result;
        this.count = count;
        this.tag = tag;
    }

    public WorkbenchRecipeBuilder withTool(ItemLike... tool)
    {
        return withTool(Ingredient.of(tool));
    }

    public WorkbenchRecipeBuilder withTool(TagKey<Item> tool)
    {
        return withTool(Ingredient.of(tool));
    }

    public WorkbenchRecipeBuilder withTool(ToolAction tool)
    {
        return withTool(ToolActionIngredient.fromTool(tool));
    }

    public WorkbenchRecipeBuilder withTool(Ingredient tool)
    {
        this.tool = tool;
        return this;
    }
    public WorkbenchRecipeBuilder withSecondTool(ItemLike... second_tool)
    {
        return withSecondTool(Ingredient.of(second_tool));
    }

    public WorkbenchRecipeBuilder withSecondTool(TagKey<Item> second_tool)
    {
        return withSecondTool(Ingredient.of(second_tool));
    }

    public WorkbenchRecipeBuilder withSecondTool(ToolAction second_tool)
    {
        return withTool(ToolActionIngredient.fromSecondTool(second_tool));
    }

    public WorkbenchRecipeBuilder withSecondTool(Ingredient second_tool)
    {
        this.second_tool = second_tool;
        return this;
    }

    public WorkbenchRecipeBuilder addMaterial(int count, ItemLike... x)
    {
        return addMaterial(Ingredient.of(x), count);
    }

    public WorkbenchRecipeBuilder addMaterial(ItemLike x, int count)
    {
        return addMaterial(Ingredient.of(x), count);
    }

    public WorkbenchRecipeBuilder addMaterial(TagKey<Item> x, int count)
    {
        return addMaterial(Ingredient.of(x), 1);
    }

    public WorkbenchRecipeBuilder addMaterial(ItemLike... x)
    {
        return addMaterial(Ingredient.of(x), 1);
    }

    public WorkbenchRecipeBuilder addMaterial(TagKey<Item> x)
    {
        return addMaterial(Ingredient.of(x), 1);
    }

    public WorkbenchRecipeBuilder addMaterial(Ingredient x)
    {
        return addMaterial(x, 1);
    }

    public WorkbenchRecipeBuilder addMaterial(Ingredient x, int count)
    {
        if (materials.size() >= 4)
        {
            throw new IllegalArgumentException("There can only be up to 4 materials!");
        }
        if (count <= 0)
        {
            throw new IllegalArgumentException("Count must be a positive integer!");
        }
        materials.add(WorkbenchRecipe.Material.of(x, count));
        return this;
    }

    public WorkbenchRecipeBuilder setGroup(String groupIn)
    {
        this.group = groupIn;
        return this;
    }

    public void build(Consumer<FinishedRecipe> consumerIn)
    {
        //noinspection deprecation
        this.build(consumerIn, Registry.ITEM.getKey(this.result));
    }

    public void build(Consumer<FinishedRecipe> consumerIn, ResourceLocation id)
    {
        this.validate(id);
        consumerIn.accept(createFinishedRecipe(id, this.group == null ? "" : this.group, this.result, this.count, this.tag, this.tool, this.second_tool, this.materials));
    }

    protected FinishedRecipe createFinishedRecipe(ResourceLocation id, String group, Item result, int count, CompoundTag tag, Ingredient tool, Ingredient second_tool, List<WorkbenchRecipe.Material> materials)
    {
        return new WorkbenchRecipeBuilder.Result(id, group, result, count, tag, tool, second_tool, materials);
    }

    private void validate(ResourceLocation id)
    {
        if (this.materials.isEmpty())
        {
            throw new IllegalStateException("No ingredients for crafting recipe " + id);
        }
    }

    protected static class Result implements FinishedRecipe
    {
        private final ResourceLocation id;
        private final Item result;
        private final int count;
        @Nullable
        private final CompoundTag tag;
        private final String group;
        @Nullable
        private final Ingredient tool;
        @Nullable
        private final Ingredient second_tool;
        private final List<WorkbenchRecipe.Material> materials;

        public Result(
            ResourceLocation id, String group, Item result, int count, @Nullable CompoundTag tag,
            @Nullable Ingredient tool, @Nullable Ingredient second_tool, List<WorkbenchRecipe.Material> materials
        )
        {
            this.id = id;
            this.result = result;
            this.count = count;
            this.tag = tag;
            this.group = group;
            this.tool = tool;
            this.second_tool = second_tool;
            this.materials = materials;
        }

        @Override
        public void serializeRecipeData(JsonObject recipeJson)
        {
            if (!this.group.isEmpty())
            {
                recipeJson.addProperty("group", this.group);
            }

            JsonArray jsonarray = new JsonArray();
            for (WorkbenchRecipe.Material material : this.materials)
            {
                jsonarray.add(material.serialize());
            }
            recipeJson.add("materials", jsonarray);

            if (tool != null)
            {
                recipeJson.add("tool", tool.toJson());
            }

            if (second_tool != null)
            {
                recipeJson.add("second_tool", second_tool.toJson());
            }

            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("item", Registry.ITEM.getKey(this.result).toString());
            if (this.count > 1)
            {
                resultJson.addProperty("count", this.count);
            }
            if (this.tag != null)
            {
                CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, tag).result().ifPresent(
                        result -> resultJson.add("nbt", result)
                );
            }
            recipeJson.add("result", resultJson);
        }

        @Override
        public ResourceLocation getId()
        {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType()
        {
            return WorkbenchRecipe.SERIALIZER;
        }

        @org.jetbrains.annotations.Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return null;
        }

        @org.jetbrains.annotations.Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return null;
        }

    }
}
