package dev.wintersky.workbench.api;

import com.google.gson.JsonObject;
import dev.wintersky.workbench.WorkbenchMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.common.crafting.VanillaIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ToolActionIngredient extends Ingredient
{
    public static final ResourceLocation NAME = WorkbenchMod.location("tool_ingredient");

    public static ToolActionIngredient fromTool(ToolAction toolType)
    {
        return new ToolActionIngredient(toolType);
    }

    public static ToolActionIngredient fromSecondTool(ToolAction toolType)
    {
        return new ToolActionIngredient(toolType);
    }

    protected ToolActionIngredient(ToolAction toolType)
    {
        super(Stream.of(new ItemList(toolType)));
    }

    private record ItemList(ToolAction toolType) implements Value
    {

        @Override
        public Collection<ItemStack> getItems()
        {
            return ForgeRegistries.ITEMS.getValues()
                    .stream()
                    .map(ItemStack::new)
                    .filter(stack -> stack.canPerformAction(toolType))
                    .collect(Collectors.toList());
        }

        @Override
        public JsonObject serialize()
        {
            JsonObject object = new JsonObject();
            object.addProperty("type", NAME.toString());
            object.addProperty("tool_type", toolType.name());
            return object;
        }
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer()
    {
        return Serializer.INSTANCE;
    }

    public static class Serializer extends VanillaIngredientSerializer
    {
        public static final IIngredientSerializer<? extends Ingredient> INSTANCE = new Serializer();

        @Override
        public Ingredient parse(JsonObject json)
        {
            return new ToolActionIngredient(
                    ToolAction.get(GsonHelper.getAsString(json, "tool_type"))
            );
        }
    }
}
