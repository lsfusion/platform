package lsfusion.server.base.version.impl;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.*;
import lsfusion.server.base.version.interfaces.NFList;

import java.util.function.Function;
import java.util.function.Predicate;

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
        return getNFList(version, false);
    }

    @Override
    public ImList<T> getNFCopyList(Version version) {
        return getNFList(version, true);
    }

    public ImList<T> getNFList(Version version, boolean allowRead) {
        ImList<T> result = proceedVersionFinal(version, allowRead);
        if(result!=null)
            return result;
            
        final MList<T> mList = ListFact.mList();
        proceedChanges(change -> change.proceedList(mList, version), version);
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

    public static <T> ImList<T> getNFCopyList(NFList<T> list, Version version) {
        return list.getNFCopyList(version);
    }

    @Override
    public void add(NFList<T> element, Function<T, T> mapper, Version version) {
        addChange(new NFListCopy<>(element, mapper), version);
    }

    public void removeAll(Predicate<T> filter, Version version) {
        addChange(new NFRemoveAll<>(filter), version);
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
