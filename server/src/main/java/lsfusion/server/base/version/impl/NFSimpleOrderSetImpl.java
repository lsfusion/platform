package lsfusion.server.base.version.impl;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.NFCopy;
import lsfusion.server.base.version.interfaces.NFCol;
import lsfusion.server.base.version.interfaces.NFList;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.base.version.interfaces.NFSet;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;

public class NFSimpleOrderSetImpl<T> implements NFOrderSet<T> {

    // the children of a simple group - the private one gets every action and property the parse makes, from every parse
    // thread at once -, so an add takes no lock : the parse threads queued on the lock of a synchronized list. In the order
    // of the adds, as that list; an iteration sees what was added before it began, and maybe what is added meanwhile
    private final ConcurrentLinkedDeque<T> list = new ConcurrentLinkedDeque<>();

    public NFSimpleOrderSetImpl(ImList<T> startList) {
        list.addAll(startList.toJavaList());
    }

    public Iterable<T> getNFIt(Version version) {
        return getIt();
    }

    @Override
    public Iterable<T> getNFIt(Version version, boolean allowRead) {
        return getIt();
    }

    boolean iterated;
    public Iterable<T> getIt() {
        iterated = true;
        return Collections.unmodifiableCollection(list);
    }

    public void add(T element, Version version) {
        list.add(element);
    }

    @Override
    public void add(NFCol<T> element, NFCopy.Map<T> mapper, Version version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(NFOrderSet<T> element, NFCopy.Map<T> mapper, Version version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(NFList<T> element, NFCopy.Map<T> mapper, Version version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(NFSet<T> element, NFCopy.Map<T> mapper, Version version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImOrderSet<T> getNFCopyOrderSet(Version version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImList<T> getNFCopyList(Version version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImSet<T> getNFCopySet(Version version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<T> getNFCopyIt(Version version) {
        throw new UnsupportedOperationException();
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
        list.removeLastOccurrence(element); // from the end : what leaves - a property declared IN a group - was added just before
    }

    public void removeAll(Predicate<T> filter, Version version) {
        assert false;
        list.removeIf(filter);
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
        return SetFact.fromJavaOrderSet(new ArrayList<>(list)); // one pass : the deque's size is counted, and it can grow meanwhile
    }

    @Override
    public int size(Version version) {
        return list.size();
    }

    public ImOrderSet<T> getNFOrderSet(Version version) {
        throw new UnsupportedOperationException();
    }

    public void finalizeChanges() {
    }

    @Override
    public boolean isFinal() {
        return false;
    }
}
