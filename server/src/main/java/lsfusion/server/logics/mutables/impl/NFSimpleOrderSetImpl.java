package lsfusion.server.logics.mutables.impl;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.mutables.FindIndex;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFOrderSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class NFSimpleOrderSetImpl<T> implements NFOrderSet<T> {

    private final List<T> list = Collections.synchronizedList(new ArrayList<T>());

    public NFSimpleOrderSetImpl(ImList<T> startList) {
        list.addAll(startList.toJavaList());
    }

    public Iterable<T> getNFIt(Version version) {
        return getIt();
    }

    boolean iterated;
    private final Iterable<T> it = new Iterable<T>() {
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                private int i;

                public boolean hasNext() {
                    return i<list.size();
                }

                public T next() {
                    return list.get(i++);
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }; 
    public Iterable<T> getIt() {
        iterated = true;
        return it;
    }

    public void add(T element, Version version) {
        list.add(element);
    }

    public void finalizeCol() {
        throw new UnsupportedOperationException();
    }

    public ImSet<T> getSet() {
        return getOrderSet().getSet();
    }

    public boolean containsNF(T element, Version version) {
        assert false;
        return list.contains(element);
    }

    public ImSet<T> getNFSet(Version version) {
        assert false;
        return getSet();
    }

    public void remove(T element, Version version) {
        assert !iterated;
        list.remove(element);
    }

    public void removeAll(Version version) {
        assert false;
        list.clear();
    }

    public ImList<T> getList() {
        return getOrderSet();
    }

    public Iterable<T> getListIt() {
        return getIt();
    }

    public Iterable<T> getNFListIt(Version version) {
        return getIt();
    }

    public ImList<T> getNFList(Version version) {
        return getList();
    }

    public ImOrderSet<T> getOrderSet() {
        return SetFact.fromJavaOrderSet(list);
    }

    @Override
    public int size(Version version) {
        return list.size();
    }

    public void addIfNotExistsToThenLast(T element, T to, boolean isRightNeighbour, Version version) {
        throw new UnsupportedOperationException();
    }

    public void move(T element, T otherElement, boolean isRightNeighbour, Version version) {
        throw new UnsupportedOperationException();
    }

    public void move(T element, FindIndex<T> finder, Version version) {
        throw new UnsupportedOperationException();
    }

    public void add(T element, FindIndex<T> finder, Version version) {
        throw new UnsupportedOperationException();
    }

    public void addFirst(T element, Version version) {
        throw new UnsupportedOperationException();
    }

    public ImOrderSet<T> getNFOrderSet(Version version) {
        throw new UnsupportedOperationException();
    }

    public void finalizeChanges() {
    }
}
