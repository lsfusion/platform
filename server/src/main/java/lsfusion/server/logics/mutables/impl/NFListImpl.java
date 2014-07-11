package lsfusion.server.logics.mutables.impl;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.impl.changes.NFListChange;
import lsfusion.server.logics.mutables.impl.changes.NFRemoveAll;
import lsfusion.server.logics.mutables.interfaces.NFList;

public class NFListImpl<T> extends NFAColImpl<T, NFListChange<T>, ImList<T>> implements NFList<T> {

    public NFListImpl() {
    }

    public NFListImpl(ImList<T> changes) {
        super(changes);
    }

    protected ImCol<T> getFinalCol(ImList<T> fcol) {
        return fcol.getCol();
    }

    public ImList<T> getNFList(Version version) {
        ImList<T> result = proceedVersionFinal(version);
        if(result!=null)
            return result;
            
        final MList<T> mList = ListFact.mList();
        proceedChanges(new ChangeProcessor<T, NFListChange<T>>() {
            public void proceed(NFListChange<T> change) {
                change.proceedList(mList);
            }
        }, version);
        return mList.immutableList();
    }

    public ImList<T> getNF(Version version) {
        return getNFList(version);
    }

    public ImList<T> getList() {
        return getFinal();
    }

    public Iterable<T> getListIt() {
        return getListIt(this);
    }

    public Iterable<T> getNFListIt(Version version) {
        return getNFListIt(this, version);
    }
    
    // множественное наследование
    
    public static <T> Iterable<T> getListIt(NFList<T> list) {
        return list.getList();
    }

    public static <T> Iterable<T> getNFListIt(NFList<T> list, Version version) {
        return list.getNFList(version);
    }

    public void removeAll(Version version) {
        addChange(NFRemoveAll.<T>getInstance(), version);
    }
}
