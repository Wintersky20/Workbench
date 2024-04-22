package dev.wintersky.workbench.table;

import com.google.common.collect.Lists;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

public class ListenableHolder
{
    private final List<Reference<? extends WorkbenchContainer>> listeners = Lists.newArrayList();
    private final ReferenceQueue<WorkbenchContainer> pendingRemovals = new ReferenceQueue<>();

    public void addWeakListener(WorkbenchContainer e)
    {
        listeners.add(new WeakReference<>(e, pendingRemovals));
    }

    public void doCallbacks()
    {
        for (Reference<? extends WorkbenchContainer>
             ref = pendingRemovals.poll();
             ref != null;
             ref = pendingRemovals.poll())
        {
            listeners.remove(ref);
        }

        for (Iterator<Reference<? extends WorkbenchContainer>> iterator = listeners.iterator(); iterator.hasNext(); )
        {
            Reference<? extends WorkbenchContainer> reference = iterator.next();
            WorkbenchContainer listener = reference.get();
            if (listener == null)
                iterator.remove();
            else
                listener.onInventoryChanged();
        }
    }
}
