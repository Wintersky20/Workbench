package dev.wintersky.workbench;

import dev.wintersky.workbench.api.WorkbenchRecipe;
import dev.wintersky.workbench.api.ToolActionIngredient;
import dev.wintersky.workbench.tool_box.ToolBox;
import dev.wintersky.workbench.table.WorkbenchBlock;
import dev.wintersky.workbench.table.WorkbenchContainer;
import dev.wintersky.workbench.table.WorkbenchScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

@Mod(WorkbenchMod.MODID)
public class WorkbenchMod
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "workbench";

    public static final CreativeModeTab WORKBENCH_TAB = new CreativeModeTab("workbench_tab")
    {
        @Override
        public ItemStack makeIcon()
        {
            return new ItemStack(WorkbenchMod.TOOL_BOX.get());
        }
    };

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    public static final RegistryObject<Item> TOOL_BOX = ITEMS.register("tool_box",
            () -> new ToolBox(new Item.Properties().durability(4096).tab(WORKBENCH_TAB))
    );

    public static final RegistryObject<Block> WORKBENCH_BLOCK = BLOCKS.register("workbench",
            () -> new WorkbenchBlock(
                    BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).noOcclusion()
    ));

    public static final RegistryObject<Item> WORKBENCH_ITEM = ITEMS.register("workbench",
            () -> new BlockItem(WORKBENCH_BLOCK.get(), new Item.Properties().tab(WORKBENCH_TAB))
    );

    public WorkbenchMod()
    {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::processIMC);
        modBus.addListener(this::gatherData);
        modBus.addGenericListener(RecipeSerializer.class, this::registerRecipes);
        modBus.addGenericListener(MenuType.class, this::registerContainers);

        ITEMS.register(modBus);
        BLOCKS.register(modBus);
    }

    private void registerRecipes(RegistryEvent.Register<RecipeSerializer<?>> event)
    {
        CraftingHelper.register(ToolActionIngredient.NAME, ToolActionIngredient.Serializer.INSTANCE);

        event.getRegistry().registerAll(
                new WorkbenchRecipe.Serializer().setRegistryName("crafting")
        );
    }

    private void registerContainers(RegistryEvent.Register<MenuType<?>> event)
    {
        event.getRegistry().registerAll(
                new MenuType<>(WorkbenchContainer::new).setRegistryName("workbench")
        );
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m -> m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }

    private void gatherData(GatherDataEvent event)
    {
        WorkbenchDataGen.gatherData(event);
    }

    public static ResourceLocation location(String path)
    {
        return new ResourceLocation(MODID, path);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBus
    {
        @SubscribeEvent
        public static void clientSetup(final FMLClientSetupEvent event)
        {
            event.enqueueWork(() -> {
                MenuScreens.register(WorkbenchContainer.TYPE, WorkbenchScreen::new);
            });
            WorkbenchScreen.register();
        }

        @SubscribeEvent
        public static void textureStitch(final TextureStitchEvent.Pre event)
        {
            //noinspection deprecation
            if (event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS))
            {
                event.addSprite(location("gui/tool_slot_background"));
                event.addSprite(location("gui/2nd_tool_slot_background"));
            }
        }

        @SubscribeEvent
        public static void modelRegistry(final ModelRegistryEvent event)
        {
        }

    }
}
