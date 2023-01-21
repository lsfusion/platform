package lsfusion.server.base.version.impl;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.NFOrderSetChange;
import lsfusion.server.base.version.impl.changes.NFRemoveAll;
import lsfusion.server.base.version.interfaces.NFOrderSet;

import java.util.List;

public class NFOrderSetImpl<T> extends NFASetImpl<T, NFOrderSetChange<T>, ImOrderSet<T>> implements NFOrderSet<T> {

    public NFOrderSetImpl() {
    }

    public NFOrderSetImpl(boolean allowVersionFinalRead) {
        super(allowVersionFinalRead);
    }

    public ImOrderSet<T> getNF(Version version) {
        ImOrderSet<T> result = proceedVersionFinal(version);
        if(result!=null)
            return result;

        final List<T> mSet = SetFact.mAddRemoveOrderSet();
        proceedChanges(change -> change.proceedOrderSet(mSet), version);
        return SetFact.fromJavaOrderSet(mSet);
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

    // множественное наследование

    public void removeAll(Version version) {
        addChange(NFRemoveAll.getInstance(), version);
    }

    public Iterable<T> getListIt() {
        return NFListImpl.getListIt(this);
    }

    public Iterable<T> getNFListIt(Version version) {
        return NFListImpl.getNFListIt(this, version);
    }

    protected ImSet<T> getFinalSet(ImOrderSet<T> fcol) {
        return fcol.getSet();
    }
}
