package lsfusion.server.base.version.impl;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.version.Version;

import java.util.Map;
import java.util.TreeMap;

public abstract class NFChangeImpl<CH, F> extends NFImpl<TreeMap<Version, MList<CH>>, F> {
    
    protected NFChangeImpl() {
        super();
    }

    protected NFChangeImpl(F changes) {
        super(changes);
    }

    // an NF cell that was never written is the common case by far - a form component alone carries dozens of them - and materializing one
    // means allocating an accumulator, walking an empty map's entry set and building an immutable copy of nothing
    protected boolean isEmptyChanges(Version version) {
        if(version != Version.last()) { // same monitor proceedChanges takes below : a write that completed before this read has to be visible to it
            synchronized (this) {
                return getChanges() == null;
            }
        }
        return getChanges() == null; // addChange is the only thing that creates the map, and it always puts something in it
    }

    protected interface ChangeProcessor<CH> {
        void proceed(CH change, CH nextChange);
    }
    
    protected void proceedChanges(ChangeProcessor<CH> processor, Version version) {
        if(version != Version.last()) {
            synchronized (this) {
                syncProceedChanges(processor, version);
            }
        } else
            syncProceedChanges(processor, version);
    }

    private void syncProceedChanges(ChangeProcessor<CH> processor, Version version) {
        TreeMap<Version, MList<CH>> changes = getChanges();
        if(changes == null)
            return;

        for(Map.Entry<Version, MList<CH>> change : changes.entrySet()) {
            if(change.getKey().compareTo(version) > 0) // если более поздняя версия
                break;
            if(!version.canSee(change.getKey()))
                continue;

            MList<CH> list = change.getValue();
            for(int i=0,size=list.size();i<size;i++)
                processor.proceed(list.get(i), i + 1 < size ? list.get(i + 1) : null);
        }
    }

    protected synchronized void addChange(CH change, Version version) {
        TreeMap<Version, MList<CH>> mChanges = getChanges(); // still goes through getChanges so a restarted collection is caught
        boolean created = mChanges == null;
        if(created)
            mChanges = new TreeMap<>();

        MList<CH> mList = mChanges.get(version);
        if(mList == null) {
            mList = ListFact.mList();
            mChanges.put(version, mList);
        }
        mList.add(change);

        if(created) // published only once it holds the change : the readers that skip the monitor for Version.last() synchronize on this write
            setChanges(mChanges);
    }
}
