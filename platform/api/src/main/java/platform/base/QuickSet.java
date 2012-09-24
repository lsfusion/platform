package platform.base;

import java.util.*;

public class QuickSet<T> implements Iterable<T>, FunctionSet<T> {
    public int size;
    protected Object[] table;
    protected int[] htable;

    protected int[] indexes;

    private final float loadFactor;
    public QuickSet() {
        loadFactor = 0.3f;

        table = new Object[8];
        htable = new int[table.length];

        indexes = new int[(int)(table.length * loadFactor)];
    }

    private final static QuickSet EMPTY = new QuickSet();
    public static <T> QuickSet<T> EMPTY() {
        return EMPTY;
    }
    public static <T> QuickSet<T> add(QuickSet<T>... sets) {
        QuickSet<T> result = new QuickSet<T>(sets[0]);
        for(int i=1;i<sets.length;i++)
            result.addAll(sets[i]);
        return result;
    }

    public QuickSet(QuickSet<T> set) {
        size = set.size;
        loadFactor = set.loadFactor;

        table = set.table.clone();
        htable = set.htable.clone();

        indexes = set.indexes.clone();
    }

    public QuickSet(T element) {
        this();
        add(element);
    }

    public QuickSet(T... elements) {
        this();
        addAll(elements);
    }

    public QuickSet(T[] elements, int size) {
        this();
        addAll(elements, size);
    }

    public <V> QuickSet(int size, V[] elements) {
        this();
        addAll(size, elements);
    }

    public QuickSet(Iterable<? extends T> col) {
        this();
        addAll(col);
    }

    public QuickSet(Collection<? extends T> col, T element) {
        this();
        addAll(col);
        add(element);
    }

    public Set<T> toSet() {
        Set<T> result = new HashSet<T>();
        for(int i=0;i<size;i++)
            result.add(get(i));
        return result;
    }

    public boolean contains(T where) {
        return contains(where,hash(where.hashCode()));
    }

    public boolean containsAll(QuickSet<? extends T> wheres) {
        for(int i=0;i<wheres.size;i++)
            if(!contains(wheres.get(i)))
                return false;
        return true;
    }

    private boolean contains(T where,int hash) {
        for(int i=hash & (table.length-1);table[i]!=null;i=(i==table.length-1?0:i+1))
            if(htable[i]==hash && table[i].equals(where))
                return true;
        return false;
    }

    public boolean intersect(QuickSet<T> set) {
        if(size>set.size) return set.intersect(this);

        for(int i=0;i<size;i++)
            if(set.contains(get(i),htable[indexes[i]]))
                return true;
        return false;
    }

    public boolean intersect(FunctionSet<T> set) {
        if(set instanceof QuickSet)
            return intersect((QuickSet<T>)set);

        if(set.isEmpty() || isEmpty())
            return false;

        if(set.isFull())
            return true;

        for(int i=0;i<size;i++)
            if(set.contains(get(i)))
                return true;
        return false;
    }

    public boolean isFull() {
        return false;
    }

    public QuickSet(QuickSet<T>[] sets) {
        QuickSet<T> minSet = sets[0];
        for(int i=1;i<sets.length;i++)
            if(sets[i].size<minSet.size)
                minSet = sets[i];

        loadFactor = 0.3f;

        table = new Object[minSet.table.length];
        htable = new int[table.length];

        indexes = new int[(int)(table.length * loadFactor)];

        for(int i=0;i<minSet.size;i++) {
            T element = minSet.get(i); int hash = minSet.htable[minSet.indexes[i]];
            boolean all = true;
            for(QuickSet<T> set : sets)
                if(set!=minSet && !set.contains(element,hash)) {
                    all = false;
                    break;
                }
            if(all)
                add(element,hash);
        }
    }

    public boolean isEmpty() {
        return size==0;
    }

    private void resize(int length) {
        int[] newIndexes = new int[(int)(length * loadFactor)];

        Object[] newTable = new Object[length];
        int[] newHTable = new int[length];
        for(int i=0;i<size;i++) {
            int newHash = (htable[indexes[i]] & (length-1));
            while(newTable[newHash]!=null) newHash = (newHash==length-1?0:newHash+1);
            newTable[newHash] = table[indexes[i]];
            newHTable[newHash] = htable[indexes[i]];

            newIndexes[i] = newHash;
        }

        table = newTable;
        htable = newHTable;

        indexes = newIndexes;
    }

    public static int hash(int h) { // копися с hashSet'а
        h ^= (h >>> 20) ^ (h >>> 12);
        return (h ^ (h >>> 7) ^ (h >>> 4));
    }

    public boolean add(T where) {
        return add(where,hash(where.hashCode()));
    }

    protected boolean add(T where,int hash) {
        int i=hash & (table.length-1);
        while(table[i]!=null) {
            if(htable[i]==hash && (table[i]==where || table[i].equals(where)))
                return true;
            i=(i==table.length-1?0:i+1);
        }
        table[i] = where; htable[i] = hash;
        indexes[size++] = i;
        if(size>=indexes.length)
            resize(2*table.length);
        return false;
    }

    public void addAll(QuickSet<? extends T> set) {
        for(int i=0;i<set.size;i++)
            add(set.get(i),set.htable[set.indexes[i]]);
    }

    public void addAll(Iterable<? extends T> col) {
        for(T element : col)
            add(element);
    }

    public void addAll(T... col) {
        for(T element : col)
            add(element);
    }

    public void addAll(T[] col, int size) {
        for(int i=0;i<size;i++)
            add(col[i]);
    }

    public <V> void addAll(int size, V[] col) {
        for(int i=0;i<size;i++)
            add((T)col[i]);
    }


    public T get(int i) {
        return (T) table[indexes[i]];
    }

    @Override
    public String toString() {
        String result = "";
        for(int i=0;i<size;i++)
            result = (result.length()==0?"":result+" - ") + table[indexes[i]];
        return result;
    }

    public T getSingle() {
        if(size==1)
            return get(0);
        else
            return null;
    }

    @Override
    public boolean equals(Object obj) {
        if(this==obj) return true;
        if(getClass()!=obj.getClass()) return false;

        QuickSet set = (QuickSet)obj;
        if(set.size!=size) return false;

        for(int i=0;i<size;i++)
            if(!set.contains(table[indexes[i]])) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for(int i=0;i<size;i++)
            hash = hash + htable[indexes[i]];
        return hash * 31;
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int i=0;

            public boolean hasNext() {
                return i<size;
            }

            @Override
            public T next() {
                return (T) table[indexes[i++]];
            }

            @Override
            public void remove() {
                throw new RuntimeException();
            }
        };
    }

    public QuickSet<T> merge(QuickSet<T> merge) {
        if(merge.size==0) return this;
        if(size==0) return merge;

        QuickSet<T> result = new QuickSet<T>(this);
        result.addAll(merge);
        return result;
    }

    public QuickSet<T> merge(T element) {
        QuickSet<T> result = new QuickSet<T>(this);
        result.add(element);
        return result;
    }

    public QuickSet<T> remove(QuickSet<T> remove) {
        if(remove.size==0 || size==0) return this;

        QuickSet<T> result = new QuickSet<T>();
        for(int i=0;i<size;i++) {
            T where = get(i);
            int hash = htable[indexes[i]];
            if(!remove.contains(where, hash))
                result.add(where, hash);
        }
        return result;
    }

    public Map<T, String> mapString() {
        Map<T, String> result = new HashMap<T, String>();
        for (T element : this)
            result.put(element, element.toString());
        return result;
    }

    public boolean disjoint(Collection<T> col) {
        for(T element : col)
            if(contains(element))
                return false;
        return true;
    }

    public boolean disjoint(QuickSet<T> col) {
        for(int i=0;i<col.size;i++)
            if(contains(col.get(i)))
                return false;
        return true;
    }

    public boolean containsAll(Collection<? extends T> col) {
        for(T element : col)
            if(!contains(element))
                return false;
        return true;
    }

    public Map<T, T> toMap() {
        Map<T, T> result = new HashMap<T, T>();
        for(T element : this)
            result.put(element, element);
        return result;
    }

    public <V> Map<T, V> toMap(V value) {
        Map<T, V> result = new HashMap<T, V>();
        for(T element : this)
            result.put(element, value);
        return result;
    }

    public <V> QuickMap<T, V> toQuickMap(V value) {
        QuickMap<T, V> result = new SimpleMap<T, V>();
        for(T element : this)
            result.add(element, value);
        return result;
    }

    public Set<T> getSet() {
        Set<T> result = new HashSet<T>();
        for(T element : this)
            result.add(element);
        return result;
    }
}
