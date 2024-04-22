package dev.wintersky.workbench.table;

import com.google.common.collect.Lists;
import dev.wintersky.workbench.WorkbenchMod;
import dev.wintersky.workbench.api.WorkbenchRecipe;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import net.minecraftforge.registries.ObjectHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WorkbenchContainer extends AbstractContainerMenu
{
    @ObjectHolder("workbench:workbench")
    public static MenuType<WorkbenchContainer> TYPE;

    private static final int NUM_INPUTS = 6;
    private static final int NUM_OUTPUTS = 1;
    private static final int NUM_INVENTORY = 9 * 3;
    private static final int NUM_HOTBAR = 9;
    private static final int OUTPUTS_START = NUM_INPUTS;
    private static final int PLAYER_START = OUTPUTS_START + NUM_OUTPUTS;
    private static final int HOTBAR_START = PLAYER_START + NUM_INVENTORY;
    private static final int PLAYER_END = HOTBAR_START + NUM_HOTBAR;

    private final Level world;
    private final ContainerLevelAccess openedFrom;
    private final DataSlot selectedRecipe = DataSlot.standalone();
    private final ItemStack[] inputStacksCache = new ItemStack[]{
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };

    private final InventoryProvider inventoryProvider;
    private List<WorkbenchRecipe> recipes = Lists.newArrayList();
    private long lastTimeSoundPlayed;

    private Runnable inventoryUpdateListener = () -> {
    };

    public final IItemHandlerModifiable inputInventory;
    /**
     * The inventory that stores the output of the crafting recipe.
     */
    private final ResultContainer inventory = new ResultContainer();

    public WorkbenchContainer(int windowIdIn, Inventory playerInventoryIn)
    {
        this(windowIdIn, playerInventoryIn, ContainerLevelAccess.NULL);
    }

    public WorkbenchContainer(int windowIdIn, Inventory playerInventoryIn, final ContainerLevelAccess worldPosCallableIn)
    {
        this(windowIdIn, playerInventoryIn, worldPosCallableIn, new WorkbenchInventory());
    }

    public WorkbenchContainer(int windowIdIn, Inventory playerInventoryIn, final ContainerLevelAccess worldPosCallableIn, InventoryProvider inventoryProvider)
    {
        super(TYPE, windowIdIn);
        this.openedFrom = worldPosCallableIn;
        this.world = playerInventoryIn.player.level;
        this.inputInventory = inventoryProvider.getInventory();
        this.inventoryProvider = inventoryProvider;
        inventoryProvider.addWeakListener(this);

        this.addSlot(new SlotItemHandler(this.inputInventory, 0, 8, 15)
        {
            {
                this.setBackground(InventoryMenu.BLOCK_ATLAS, WorkbenchMod.location("gui/tool_slot_background"));
            }
        });
        this.addSlot(new SlotItemHandler(this.inputInventory, 1, 30, 15)
        {
            {
                this.setBackground(InventoryMenu.BLOCK_ATLAS, WorkbenchMod.location("gui/2nd_tool_slot_background"));
            }
        });
        this.addSlot(new SlotItemHandler(this.inputInventory, 2, 10, 35));
        this.addSlot(new SlotItemHandler(this.inputInventory, 3, 28, 35));
        this.addSlot(new SlotItemHandler(this.inputInventory, 4, 10, 53));
        this.addSlot(new SlotItemHandler(this.inputInventory, 5, 28, 53));
        this.addSlot(new Slot(this.inventory, 1, 143, 33)
        {
            /**
             * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
             */
            public boolean mayPlace(ItemStack stack)
            {
                return false;
            }

            public void onTake(Player thePlayer, ItemStack stack)
            {
                if (!thePlayer.level.isClientSide)
                {
                    stack.onCraftedBy(thePlayer.level, thePlayer, stack.getCount());
                    WorkbenchContainer.this.inventory.awardUsedRecipes(thePlayer);

                    WorkbenchRecipe recipe = recipes.get(getSelectedRecipe());
                    Map<Ingredient, Integer> remaining = recipe.getMaterials().stream().collect(Collectors.toMap(i -> i.ingredient, i -> i.count));
                    if (consumeCraftingMaterials(thePlayer, remaining))
                    {
                        updateRecipeResultSlot();
                    }

                    worldPosCallableIn.execute((world, pos) -> {
                        long l = world.getGameTime();
                        if (lastTimeSoundPlayed != l)
                        {
                            world.playSound(null, pos, SoundEvents.WOOD_STEP, SoundSource.BLOCKS, 1.0F, 1.0F);
                            lastTimeSoundPlayed = l;
                        }
                    });
                }
                super.onTake(thePlayer, stack);
            }
        });

        bindPlayerInventory(playerInventoryIn);

        this.addDataSlot(this.selectedRecipe);
    }

    private boolean consumeCraftingMaterials(Player thePlayer, Map<Ingredient, Integer> remaining)
    {
        boolean needsUpdate = false;
        for (int i = 0; i < 6; i++)
        {
            Slot slot = slots.get(i);
            ItemStack itemstack;
            if (i == 0)
            {
                slot.getItem().hurtAndBreak(1, thePlayer, player -> {
                    slot.remove(1);
                });
                itemstack = slot.getItem();
            }
            else if (i == 1)
            {
                //default value /* itemstack = ItemStack.EMPTY; */

                slot.getItem().hurtAndBreak(1, thePlayer, player -> {
                    slot.remove(1);
                });
                itemstack = slot.getItem();
            }
            else
            {
                int subtract = 0;
                for (Map.Entry<Ingredient, Integer> mat : remaining.entrySet())
                {
                    Ingredient ing = mat.getKey();
                    int value = mat.getValue();
                    ItemStack stack1 = slot.getItem();
                    if (ing.test(stack1))
                    {
                        int remaining1 = Math.max(0, value - (stack1.getCount() + subtract));
                        subtract += (value - remaining1);
                        mat.setValue(remaining1);
                    }
                }
                itemstack = slot.remove(subtract);
            }
            if (!itemstack.isEmpty())
            {
                needsUpdate = true;
            }
        }
        return needsUpdate;
    }

    private void bindPlayerInventory(Inventory playerInventoryIn)
    {
        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 9; ++j)
            {
                this.addSlot(new Slot(playerInventoryIn, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k)
        {
            this.addSlot(new Slot(playerInventoryIn, k, 8 + k * 18, 142));
        }
    }

    public void onInventoryChanged()
    {
        slotsChanged(new RecipeWrapper(inputInventory));
        inventoryUpdateListener.run();
    }

    public int getSelectedRecipe()
    {
        return this.selectedRecipe.get();
    }

    public List<WorkbenchRecipe> getRecipeList()
    {
        return this.recipes;
    }

    public int getRecipeListSize()
    {
        return this.recipes.size();
    }

    public boolean hasItemsinInputSlots()
    {
        return slots.stream().skip(2).limit(4).anyMatch(Slot::hasItem);
    }

    public boolean isAbleToCraft()
    {
        return hasItemsinInputSlots() && !this.recipes.isEmpty();
    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean stillValid(Player playerIn)
    {
        return isWithinUsableDistance(this.openedFrom, playerIn, WorkbenchMod.WORKBENCH_BLOCK.get());
    }

    protected static boolean isWithinUsableDistance(ContainerLevelAccess worldPos, Player playerIn, Block... targetBlocks)
    {
        return worldPos.evaluate((world, pos) -> {
            BlockState blockState = world.getBlockState(pos);
            if (Arrays.stream(targetBlocks).noneMatch(blockState::is)) return false;

            return playerIn.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
        }, true);
    }

    /**
     * Handles the given Button-click on the server, currently only used by enchanting. Name is for legacy.
     */
    public boolean clickMenuButton(Player playerIn, int id)
    {
        if (this.isValidRecipeIndex(id))
        {
            this.selectedRecipe.set(id);
            this.updateRecipeResultSlot();
        }

        return true;
    }

    private boolean isValidRecipeIndex(int p_241818_1_)
    {
        return p_241818_1_ >= 0 && p_241818_1_ < this.recipes.size();
    }

    @Override
    public void slotsChanged(Container inventoryIn)
    {
        super.slotsChanged(inventoryIn);
        boolean anyChanged = false;
        for (int i = 0; i < 6; i++)
        {
            ItemStack itemstack = slots.get(i).getItem();
            if (!ItemStack.matches(itemstack, this.inputStacksCache[i]))
            {
                this.inputStacksCache[i] = itemstack.copy();
                anyChanged = true;
            }
        }
        if (anyChanged)
            this.updateAvailableRecipes(inventoryIn);
    }

    private void updateAvailableRecipes(Container inventoryIn)
    {
        WorkbenchRecipe recipe = getSelectedRecipe() >= 0 && recipes.size() > 0 ? recipes.get(getSelectedRecipe()) : null;
        this.recipes.clear();
        this.selectedRecipe.set(-1);
        this.slots.get(OUTPUTS_START).set(ItemStack.EMPTY);
        if (hasItemsinInputSlots())
        {
            this.recipes = this.world.getRecipeManager().getRecipesFor(WorkbenchRecipe.CRAFTING, inventoryIn, this.world);
        }
        if (recipes.size() > 0 && recipe != null)
        {
            int index = recipes.indexOf(recipe);
            if (index >= 0)
            {
                selectedRecipe.set(index);
                updateRecipeResultSlot();
            }
        }
    }

    private void updateRecipeResultSlot()
    {
        if (!this.recipes.isEmpty() && this.isValidRecipeIndex(this.selectedRecipe.get()))
        {
            WorkbenchRecipe stonecuttingrecipe = this.recipes.get(this.selectedRecipe.get());
            this.inventory.setRecipeUsed(stonecuttingrecipe);
            this.slots.get(OUTPUTS_START).set(stonecuttingrecipe.assemble(new RecipeWrapper(this.inputInventory)));
        }
        else
        {
            this.slots.get(OUTPUTS_START).set(ItemStack.EMPTY);
        }

        this.broadcastChanges();
    }

    @OnlyIn(Dist.CLIENT)
    public void setInventoryUpdateListener(Runnable listenerIn)
    {
        this.inventoryUpdateListener = listenerIn;
    }

    /**
     * Called to determine if the current slot is valid for the stack merging (double-click) code. The stack passed in is
     * null for the initial slot that was double-clicked.
     */
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn)
    {
        return slotIn.container != this.inventory && super.canTakeItemForPickAll(stack, slotIn);
    }

    public ItemStack quickMoveStack(Player playerIn, int index)
    {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stackInSlot = slot.getItem();
        Item item = stackInSlot.getItem();
        ItemStack stackCopy = stackInSlot.copy();

        int startIndex = PLAYER_START;
        int endIndex = PLAYER_END;
        boolean reverse = false;
        boolean notify = false;
        if (index == OUTPUTS_START)
        {
            // Drop onto player inventory, reverse fill order.
            reverse = true;
            notify = true;
        }
        else if (index >= OUTPUTS_START)
        {
            if (stackInSlot.getMaxStackSize() == 1 && slots.get(0).getItem().getCount() == 0)
            {
                startIndex = 0;
                endIndex = 1;
            }
            /*else if (stackInSlot.getItem().isIn(WorkbenchMod.PATTERNS_TAG))
            {
                startIndex = 1;e
                endIndex = 2;
            }*/
            else
            {
                startIndex = 2;
                endIndex = OUTPUTS_START;
            }
        }

        if (endIndex > startIndex)
        {
            if (notify) item.onCraftedBy(stackInSlot, playerIn.level, playerIn);
            if (!this.moveItemStackTo(stackInSlot, startIndex, endIndex, reverse))
            {
                return ItemStack.EMPTY;
            }
            if (notify) slot.onQuickCraft(stackInSlot, stackCopy);
        }

        if (stackInSlot.isEmpty())
        {
            slot.set(ItemStack.EMPTY);
        }

        slot.setChanged();
        if (stackInSlot.getCount() == stackCopy.getCount())
        {
            return ItemStack.EMPTY;
        }

        slot.onTake(playerIn, stackInSlot);
        this.broadcastChanges();

        return stackCopy;
    }

    private boolean hasRecipe(ItemStack stackInSlot)
    {
        return this.world.getRecipeManager().getRecipeFor(WorkbenchRecipe.CRAFTING, new SimpleContainer(stackInSlot), this.world).isPresent();
    }

    public void removed(Player playerIn)
    {
        super.removed(playerIn);
        this.inventory.removeItemNoUpdate(0);
        if (inventoryProvider.isDummy())
        {
            this.openedFrom.execute((world, pos) -> this.clearContainer(playerIn, new RecipeWrapper(this.inputInventory)));
        }
    }
}
