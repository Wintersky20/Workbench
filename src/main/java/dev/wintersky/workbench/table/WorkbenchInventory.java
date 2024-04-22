package dev.wintersky.workbench.table;

import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

class WorkbenchInventory extends ItemStackHandler implements InventoryProvider
{
    private final ListenableHolder listenable = new ListenableHolder();

    public WorkbenchInventory()
    {
        super(6);
    }

    @Override
    protected void onContentsChanged(int slot)
    {
        super.onContentsChanged(slot);
        listenable.doCallbacks();
    }

    @Override
    public void addWeakListener(WorkbenchContainer e)
    {
        listenable.addWeakListener(e);
    }

    @Override
    public IItemHandlerModifiable getInventory()
    {
        return this;
    }

    @Override
    public boolean isDummy()
    {
        return true;
    }
}
