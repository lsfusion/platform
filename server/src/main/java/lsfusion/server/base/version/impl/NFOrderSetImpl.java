package lsfusion.server.base.version.impl;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.NFCopy;
import lsfusion.server.base.version.impl.changes.NFOrderSetChange;
import lsfusion.server.base.version.impl.changes.NFOrderSetCopy;
import lsfusion.server.base.version.impl.changes.NFRemoveAll;
import lsfusion.server.base.version.interfaces.NFList;
import lsfusion.server.base.version.interfaces.NFOrderSet;

import java.util.List;
import java.util.function.Predicate;

public class NFOrderSetImpl<T> extends NFASetImpl<T, NFOrderSetChange<T>, ImOrderSet<T>> implements NFOrderSet<T> {

    public NFOrderSetImpl() {
    }

    public NFOrderSetImpl(boolean allowVersionFinalRead) {
        super(allowVersionFinalRead);
    }

    public ImOrderSet<T> getNF(Version version) {
        return getNF(version, false);
    }

    private ImOrderSet<T> getNF(Version version, boolean allowRead) {
        ImOrderSet<T> result = proceedVersionFinal(version, allowRead);
        if(result!=null)
            return result;

        final List<T> mSet = SetFact.mAddRemoveOrderSet();
        proceedChanges(change -> change.proceedOrderSet(mSet, version), version);
        return SetFact.fromJavaOrderSet(mSet);
    }

    @Override
    public ImOrderSet<T> getNFCopyOrderSet(Version version) {
        return getNF(version, true);
    }

    public ImOrderSet<T> getNFOrderSet(Version version) {
        return getNF(version);
    }

    @Override
    public int size(Version version) {
        return getNF(version).size();
    }

    public ImSet<T> getSet() {
        return getOrderSet().getSet();
    }

    public ImList<T> getNFList(Version version) {
        return getNF(version);
    }

    public ImList<T> getList() {
        return getFinal();
    }

    public ImOrderSet<T> getOrderSet() {
        return getFinal();
    }

    @Override
    public void add(NFOrderSet<T> element, NFCopy.Map<T> mapper, Version version) {
        addChange(new NFOrderSetCopy<>(element, mapper), version);
    }


    // множественное наследование

    @Override
    public void add(NFList<T> element, NFCopy.Map<T> mapper, Version version) {
        throw new UnsupportedOperationException();
    }

    public void removeAll(Predicate<T> filter, Version version) {
        addChange(new NFRemoveAll<>(filter), version);
    }

    public Iterable<T> getListIt() {
        return NFListImpl.getListIt(this);
    }

    public Iterable<T> getNFListIt(Version version) {
        return NFListImpl.getNFListIt(this, version);
    }

    public ImList<T> getNFCopyList(Version version) {
        return NFListImpl.getNFCopyList(this, version);
    }

    protected ImSet<T> getFinalSet(ImOrderSet<T> fcol) {
        return fcol.getSet();
    }
}
