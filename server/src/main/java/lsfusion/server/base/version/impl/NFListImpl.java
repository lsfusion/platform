package lsfusion.server.base.version.impl;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.NFAddFirstList;
import lsfusion.server.base.version.impl.changes.NFListChange;
import lsfusion.server.base.version.impl.changes.NFRemoveAll;
import lsfusion.server.base.version.interfaces.NFList;

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
        proceedChanges(change -> change.proceedList(mList), version);
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
        addChange(NFRemoveAll.getInstance(), version);
    }

    public void addFirst(T element, Version version) {
        addChange(new NFAddFirstList<>(element), version);
    }

    public static <T> void add(boolean isLast, NFList<T> list, T element, Version version) {
        if(isLast)
            list.add(element, version);
        else
            list.addFirst(element, version);
    }
}
