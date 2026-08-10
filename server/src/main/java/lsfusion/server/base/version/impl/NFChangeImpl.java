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

    protected TreeMap<Version, MList<CH>> initMutable() {
        return new TreeMap<>();
    }

    // an NF cell that was never written is the common case by far - a form component alone carries dozens of them - and materializing one
    // means allocating an accumulator, walking an empty map's entry set and building an immutable copy of nothing
    protected boolean isEmptyChanges(Version version) {
        if(version != Version.last()) { // same monitor proceedChanges takes below : a write that completed before this read has to be visible to it
            synchronized (this) {
                return getChanges().isEmpty();
            }
        }
        return getChanges().isEmpty();
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
        for(Map.Entry<Version, MList<CH>> change : getChanges().entrySet()) {
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
        TreeMap<Version, MList<CH>> mChanges = getChanges();
        MList<CH> mList = mChanges.get(version);
        if(mList == null) {
            mList = ListFact.mList();
            mChanges.put(version, mList);
        }
        mList.add(change);
    }
}
