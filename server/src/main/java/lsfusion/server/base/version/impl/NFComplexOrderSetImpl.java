package lsfusion.server.base.version.impl;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.*;
import lsfusion.server.base.version.interfaces.NFComplexOrderSet;


import java.util.List;

public class NFComplexOrderSetImpl<T> extends NFChangeImpl<NFComplexOrderSetChange<T>, Pair<ImOrderSet<T>, ImList<Integer>>> implements NFComplexOrderSet<T> {

    public NFComplexOrderSetImpl() {
    }

    public NFComplexOrderSetImpl(boolean allowVersionFinalRead) {
        super(allowVersionFinalRead);
    }

    @Override
    public void add(T element, ComplexLocation<T> location, Version version) {
        addChange(new NFComplexAdd<>(element, location), version);
    }

    @Override
    public void add(NFComplexOrderSet<T> elements, NFCopy.Map<T> mapping, Version version) {
        addChange(new NFComplexOrderSetCopy<>(elements, mapping), version);
    }

    @Override
    public void remove(T element, Version version) {
        // assert included
        addChange(new NFRemove<>(element), version);
    }

    @Override
    public Pair<ImOrderSet<T>, ImList<Integer>> getNF(Version version) {
        return getNF(version, false);
    }

    @Override
    public Pair<ImOrderSet<T>, ImList<Integer>> getNFCopy(Version version) {
        return getNF(version, true);
    }

    private Pair<ImOrderSet<T>, ImList<Integer>> getNF(Version version, boolean allowRead) {
        Pair<ImOrderSet<T>, ImList<Integer>> result = proceedVersionFinal(version, allowRead);
        if(result!=null)
            return result;

        final List<T> mSet = SetFact.mAddRemoveOrderSet();
        final List<Integer> mGroup = SetFact.mAddRemoveOrderSet();
        proceedChanges(change -> change.proceedComplexOrderSet(mSet, mGroup, version), version);
        return new Pair<>(SetFact.fromJavaOrderSet(mSet), ListFact.fromJavaList(mGroup));
    }

    @Override
    protected boolean checkFinal(Object object) {
        return object instanceof Pair;
    }

    @Override
    public Iterable<T> getIt() {
        return getSet();
    }

    @Override
    public Iterable<T> getListIt() {
        return getList();
    }

    @Override
    public ImList<T> getList() {
        return getOrderSet();
    }

    @Override
    public ImSet<T> getSet() {
        return getOrderSet().getSet();
    }

    @Override
    public ImOrderSet<T> getOrderSet() {
        return getFinal().first;
    }

    @Override
    public Iterable<T> getNFIt(Version version) {
        return getNFList(version);
    }

    @Override
    public Iterable<T> getNFIt(Version version, boolean allowRead) {
        return getNF(version, true).first;
    }

    @Override
    public ImList<T> getNFList(Version version) {
        return getNF(version).first;
    }

    @Override
    public Iterable<T> getNFListIt(Version version) {
        return getNFList(version);
    }

    @Override
    public int size(Version version) {
        return getNFList(version).size();
    }
}
