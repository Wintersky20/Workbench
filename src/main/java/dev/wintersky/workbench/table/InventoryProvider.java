package dev.wintersky.workbench.table;

import net.minecraftforge.items.IItemHandlerModifiable;

public interface InventoryProvider
{
    void addWeakListener(WorkbenchContainer e);

    IItemHandlerModifiable getInventory();

    default boolean isDummy()
    {
        return false;
    }
}
