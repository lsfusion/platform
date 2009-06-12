package platform.base;

import java.util.Arrays;

public abstract class QuickSet<T,This extends QuickSet<T,This>> {
    protected int size;
    protected T[] table;
    protected int[] htable;

    protected int[] indexes;

    protected abstract T[] newArray(int size);
    protected abstract This getThis();

    private final float loadFactor;
    public QuickSet() {
        loadFactor = 0.3f;

        table = newArray(8);
        htable = new int[table.length];

        indexes = new int[(int)(table.length * loadFactor)];
    }

    public QuickSet(This set) {
        size = set.size;
        loadFactor = set.loadFactor;

        table = set.table.clone();
        htable = set.htable.clone();

        indexes = set.indexes.clone();
    }

    T[] array; // подразумевается что immutable
    public T[] toArray() {
        if(array==null) {
            array = newArray(size);
            for(int i=0;i<size;i++)
                array[i] = table[indexes[i]];
        }
        return array;
    }

    public boolean contains(T where) {
        return contains(where,hash(where.hashCode()));
    }

    private boolean contains(T where,int hash) {
        for(int i=hash & (table.length-1);table[i]!=null;i=(i==table.length-1?0:i+1))
            if(htable[i]==hash && table[i].equals(where))
                return true;
        return false;
    }

    public boolean intersect(This set) {
        if(size>set.size) return set.intersect(getThis());

        for(int i=0;i<size;i++)
            if(set.contains(table[indexes[i]],htable[indexes[i]]))
                return true;
        return false;
    }

    public QuickSet(This[] sets) {
        This minSet = sets[0];
        for(int i=1;i<sets.length;i++)
            if(sets[i].size<minSet.size)
                minSet = sets[i];

        loadFactor = 0.3f;

        table = newArray(minSet.table.length);
        htable = new int[table.length];

        indexes = new int[minSet.size];

        for(int i=0;i<minSet.size;i++) {
            T element = minSet.table[minSet.indexes[i]]; int hash = minSet.htable[minSet.indexes[i]];
            boolean all = true;
            for(This set : sets)
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

        T[] newTable = newArray(length);
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

    public void add(T where) {
        add(where,hash(where.hashCode()));
    }

    protected void add(T where,int hash) {
        int i=hash & (table.length-1);
        while(table[i]!=null) {
            if(htable[i]==hash && (table[i]==where || table[i].equals(where)))
                return;
            i=(i==table.length-1?0:i+1);
        }
        table[i] = where; htable[i] = hash;
        indexes[size++] = i;
        if(size>=indexes.length)
            resize(2*table.length);
    }

    public void addAll(This set) {
        for(int i=0;i<set.size;i++)
            add(set.table[set.indexes[i]],set.htable[set.indexes[i]]);
    }

    public T get(int i) {
        return table[indexes[i]]; 
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
            return table[indexes[0]];
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

}
