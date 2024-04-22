package dev.wintersky.workbench;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.wintersky.workbench.api.WorkbenchRecipeBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WorkbenchDataGen
{
    public static void gatherData(GatherDataEvent event)
    {
        DataGenerator gen = event.getGenerator();

        if (event.includeClient())
        {
            gen.addProvider(new Lang(gen));
            // Let blockstate provider see generated item models by passing its existing file helper
            ItemModelProvider itemModels = new ItemModels(gen, event.getExistingFileHelper());
            gen.addProvider(itemModels);
            gen.addProvider(new BlockStates(gen, itemModels.existingFileHelper));
        }
        if (event.includeServer())
        {

            var blockTags = new BlockTags(gen, event.getExistingFileHelper());
            gen.addProvider(blockTags);
            //gen.addProvider(new ItemTags(gen, blockTags));
            gen.addProvider(new Recipes(gen));
            gen.addProvider(new Loot(gen));
        }
    }

    public static class Lang extends LanguageProvider
    {
        public Lang(DataGenerator gen)
        {
            super(gen, WorkbenchMod.MODID, "en_us");
        }

        @Override
        protected void addTranslations()
        {
            add("itemGroup.workbench_tab", "Workbench");
            add("container.workbench.workbench", "Workbench");
            add("jei.category.workbench.crafting", "Crafting");

            add(WorkbenchMod.WORKBENCH_BLOCK.get(), "Workbench");
            add(WorkbenchMod.TOOL_BOX.get(), "Tool Box");

            add("text.workbench.recipe", "Required materials:");
        }
    }

    public static class BlockStates extends BlockStateProvider
    {

        public BlockStates(DataGenerator gen, ExistingFileHelper exFileHelper)
        {
            super(gen, WorkbenchMod.MODID, exFileHelper);
        }

        @Override
        protected void registerStatesAndModels()
        {
            {
                Block block = WorkbenchMod.WORKBENCH_BLOCK.get();
                horizontalBlock(block, models().getExistingFile(ModelLocationUtils.getModelLocation(block)));
            }
        }
    }

    public static class ItemModels extends ItemModelProvider
    {
        public ItemModels(DataGenerator generator, ExistingFileHelper existingFileHelper)
        {
            super(generator, WorkbenchMod.MODID, existingFileHelper);
        }

        @Override
        protected void registerModels()
        {
            getBuilder(WorkbenchMod.WORKBENCH_ITEM.getId().getPath())
                    .parent(getExistingFile(ModelLocationUtils
                            .getModelLocation(WorkbenchMod.WORKBENCH_BLOCK.get())));
        }

        private ItemModelBuilder basicIcon(ResourceLocation item)
        {
            return getBuilder(item.getPath())
                    .parent(new ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", WorkbenchMod.location("item/" + item.getPath()));
        }
    }

    private static class Recipes extends RecipeProvider
    {
        public Recipes(DataGenerator gen)
        {
            super(gen);
        }

        @Override
        protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer)
        {

            WorkbenchRecipeBuilder begin = WorkbenchRecipeBuilder.begin(Items.LEATHER_HELMET);
            begin.withTool(Tags.Items.SHEARS);
            begin.withSecondTool(Tags.Items.SHEARS);
            begin.addMaterial(Tags.Items.LEATHER, 1);
            begin.addMaterial(Tags.Items.STRING, 1);
            begin.build(consumer, WorkbenchMod.location("ex1"));

            WorkbenchRecipeBuilder begin1 = WorkbenchRecipeBuilder.begin(Items.IRON_ORE);
            begin1.withTool(WorkbenchMod.TOOL_BOX.get());
            begin1.addMaterial(Tags.Items.INGOTS_IRON, 1);
            begin1.addMaterial(Tags.Items.STONE, 1);
            begin1.build(consumer, WorkbenchMod.location("ex2"));
        }
    }

    private static class Loot extends LootTableProvider implements DataProvider
    {
        public Loot(DataGenerator gen)
        {
            super(gen);
        }

        private final List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> tables = ImmutableList.of(
                Pair.of(Loot.BlockTables::new, LootContextParamSets.BLOCK)
        );

        @Override
        protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> getTables()
        {
            return tables;
        }

        @Override
        protected void validate(Map<ResourceLocation, LootTable> map, ValidationContext validationtracker)
        {
            map.forEach((p_218436_2_, p_218436_3_) -> {
                LootTables.validate(validationtracker, p_218436_2_, p_218436_3_);
            });
        }

        public static class BlockTables extends BlockLoot
        {
            @Override
            protected void addTables()
            {
                this.dropSelf(WorkbenchMod.WORKBENCH_BLOCK.get());
            }

            @Override
            protected Iterable<Block> getKnownBlocks()
            {
                return ForgeRegistries.BLOCKS.getValues().stream()
                        .filter(b -> b.getRegistryName().getNamespace().equals(WorkbenchMod.MODID))
                        .collect(Collectors.toList());
            }
        }
    }

    private static class BlockTags extends BlockTagsProvider implements DataProvider
    {
        public BlockTags(DataGenerator gen, ExistingFileHelper existingFileHelper)
        {
            super(gen, WorkbenchMod.MODID, existingFileHelper);
        }

        @Override
        protected void addTags()
        {
            tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)
                    .add(WorkbenchMod.WORKBENCH_BLOCK.get());
        }
    }
}
