package platform.base;

import java.util.*;

public class SortedGroupSet<C extends Comparable<C>,T extends GroupInterface<C>> implements Iterable<T> {

    private SortedMap<C,Set<T>> groups = new TreeMap<C, Set<T>>();

    public SortedGroupSet() {
    }

    public SortedGroupSet(SortedGroupSet<C,T> set) {
        groups = new TreeMap<C,Set<T>>(set.groups);
    }

    public void add(T item) {
        C group = item.group();
        Set<T> groupItems = groups.get(group);
        if(groupItems==null) {
            groupItems = new HashSet<T>();
            groups.put(group,groupItems);
        }
        groupItems.add(item);
    }

    private class ItemIterator implements Iterator<T> {

        Iterator<Set<T>> groupIterator;
        Iterator<T> itemIterator;

        private ItemIterator() {
            groupIterator = groups.values().iterator();
            itemIterator = new EmptyIterator<T>();
        }

        public boolean hasNext() {
            return itemIterator.hasNext() || groupIterator.hasNext();
        }

        public T next() {
            if(!itemIterator.hasNext())
                itemIterator = groupIterator.next().iterator();
            return itemIterator.next();
        }

        public void remove() {
            throw new RuntimeException("not supported");
        }
    }

    public Iterator<T> iterator() {
        return new ItemIterator();
    }

    public void addAll(SortedGroupSet<C,T> set) {
        for(Map.Entry<C,Set<T>> group : set.groups.entrySet()) {
            Set<T> groupItems = groups.get(group.getKey());
            if(groupItems==null) {
                groupItems = new HashSet<T>();
                groups.put(group.getKey(),groupItems);
            }
            groupItems.addAll(group.getValue());
        }
    }

    public Iterable<Set<T>> groupIterator() {
        return groups.values();
    }
    public int size() {
        int size = 0;
        for(Set<T> groupItems : groups.values())
            size += groupItems.size();
        return size;
    }

    public int groupsSize() {
        return groups.size();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SortedGroupSet && groups.equals(((SortedGroupSet) o).groups);
    }

    @Override
    public int hashCode() {
        return groups.hashCode();
    }
}
