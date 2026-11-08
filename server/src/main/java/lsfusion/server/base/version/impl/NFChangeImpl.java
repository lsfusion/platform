package lsfusion.server.base.version.impl;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.version.Version;

import java.util.ArrayList;
import java.util.List;
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
                return readChanges() == null;
            }
        }
        return readChanges() == null; // addChange is the only thing that creates the map, and it always puts something in it
    }

    protected interface ChangeProcessor<CH> {
        void proceed(CH change, CH nextChange);
    }
    
    // a read at a version notes on the monitor what it would run and runs that off it : a copy among the changes maps
    // through the form's monitor (ObjectMapping.getIdentity), which a FORMS statement holds as it writes this collection
    protected void proceedChanges(ChangeProcessor<CH> processor, Version version) {
        if(version != Version.last()) {
            List<Pair<CH, CH>> changes = new ArrayList<>();
            synchronized (this) {
                syncProceedChanges((change, nextChange) -> changes.add(new Pair<>(change, nextChange)), version);
            }
            for(Pair<CH, CH> change : changes)
                proceed(processor, change.first, change.second);
        } else
            syncProceedChanges(processor, version);
    }

    private void syncProceedChanges(ChangeProcessor<CH> processor, Version version) {
        TreeMap<Version, MList<CH>> changes = readChanges();
        if(changes == null)
            return;

        for(Map.Entry<Version, MList<CH>> change : changes.entrySet()) {
            if(change.getKey().compareTo(version) > 0) // если более поздняя версия
                break;
            if(!version.canSee(change.getKey()))
                continue;

            MList<CH> list = change.getValue();
            for(int i=0,size=list.size();i<size;i++)
                proceed(processor, list.get(i), i + 1 < size ? list.get(i + 1) : null);
        }
    }

    // a restart met inside a change - a read the change makes, of another collection or of this one - is no look of
    // this read's own : made again (readCopy), the read would find this collection final and skip the rest of the
    // change, so it fails as before
    private static <CH> void proceed(ChangeProcessor<CH> processor, CH change, CH nextChange) {
        try {
            processor.proceed(change, nextChange);
        } catch (RestartedException e) {
            throw new RuntimeException(e.getMessage(), e);
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
