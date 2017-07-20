package lsfusion.server.logics.mutables.impl;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.impl.changes.NFAdd;

import java.util.Map;
import java.util.TreeMap;

public abstract class NFChangeImpl<T, CH, F> extends NFImpl<TreeMap<Version, MList<CH>>, F> {
    
    protected NFChangeImpl() {
        super();
    }

    protected NFChangeImpl(boolean allowVersionFinalRead) {
        super(allowVersionFinalRead);
    }

    protected NFChangeImpl(F changes) {
        super(changes);
    }

    protected TreeMap<Version, MList<CH>> initMutable() {
        return new TreeMap<>();
    }

    protected interface ChangeProcessor<T, CH> {
        void proceed(CH change);
    }
    
    protected void proceedChanges(ChangeProcessor<T, CH> processor, Version version) {
        if(version != Version.LAST) {
            synchronized (this) {
                syncProceedChanges(processor, version);
            }
        } else
            syncProceedChanges(processor, version);
    }

    private void syncProceedChanges(ChangeProcessor<T, CH> processor, Version version) {
        for(Map.Entry<Version, MList<CH>> change : getChanges().entrySet()) {
            if(change.getKey().compareTo(version) > 0) // если более поздняя версия
                break;
            if(!version.canSee(change.getKey()))
                continue;

            MList<CH> list = change.getValue();
            for(int i=0,size=list.size();i<size;i++)
                processor.proceed(list.get(i));
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

    public void add(T element, Version version) {
        addChange((CH) new NFAdd<>(element), version);
    }
}
