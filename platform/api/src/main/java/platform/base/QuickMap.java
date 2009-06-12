package platform.base;

// дублируем QuickSet
public abstract class QuickMap<K,V,This extends QuickMap<K,V,This>> {
    public int size;
    protected Object[] table;
    protected int[] htable;
    protected V[] vtable;

    protected int[] indexes; // номера в таблице

    protected abstract V[] newValues(int size);
    protected abstract This getThis();

    private final float loadFactor;
    public QuickMap() {
        loadFactor = 0.3f;

        table = new Object[8];
        htable = new int[table.length];
        vtable = newValues(8);

        indexes = new int[(int)(table.length * loadFactor)];
    }

    public QuickMap(This set) {
        size = set.size;
        loadFactor = set.loadFactor;

        table = set.table.clone();
        htable = set.htable.clone();
        vtable = set.vtable.clone();

        indexes = set.indexes.clone();
    }

    public K getKey(int i) {
        return (K) table[indexes[i]];
    }
    public V getValue(int i) {
        return vtable[indexes[i]];
    }

    protected abstract This copy();

    public boolean isEmpty() {
        return size==0;
    }

    private void resize(int length) {
        int[] newIndexes = new int[(int)(length * loadFactor)];

        Object[] newTable = new Object[length];
        int[] newHTable = new int[length];
        V[] newVTable = newValues(length);
        for(int i=0;i<size;i++) {
            int newHash = (htable[indexes[i]] & (length-1));
            while(newTable[newHash]!=null) newHash = (newHash==length-1?0:newHash+1);

            newTable[newHash] = table[indexes[i]];
            newHTable[newHash] = htable[indexes[i]];
            newVTable[newHash] = vtable[indexes[i]];

            newIndexes[i] = newHash;
        }

        table = newTable;
        htable = newHTable;
        vtable = newVTable;

        indexes = newIndexes;
    }

    public static int hash(int h) { // копися с hashSet'а
        h ^= (h >>> 20) ^ (h >>> 12);
        return (h ^ (h >>> 7) ^ (h >>> 4));
    }

    public boolean add(K key,V value) {
        return add(key,hash(key.hashCode()),value);
    }

    public boolean add(int index,QuickMap<K,V,?> map) {
        return add(map.table[map.indexes[index]],map.htable[map.indexes[index]],map.vtable[map.indexes[index]]);
    }

    protected abstract V addValue(V prevValue, V newValue);

    private boolean add(Object key,int hash,V value) {
        int i=hash & (table.length-1);
        while(table[i]!=null) {
            if(htable[i]==hash && table[i].equals(key)) {
                V addValue = addValue(vtable[i],value);
                if(addValue==null)
                    return false;
                else {
                    vtable[i] = addValue;
                    return true;
                }
            }
            i=(i==table.length-1?0:i+1);
        }
        table[i] = key; htable[i] = hash; vtable[i] = value;
        indexes[size++] = i; if(size>=indexes.length) resize(2*table.length);
        return true;
    }

    // здесь можно еще сократить equals не проверяя друг с другом
    public This merge(This set) {
        if(table.length>set.table.length) return set.merge(getThis()); // пусть добавляется в большую

        This result = set.copy();
        if(!result.addAll(getThis()))
            return null;
        return result;
    }

    public boolean addAll(This set) {
        for(int i=0;i<set.size;i++)
            if(!add(i,set))
                return false;
        return true;
    }

    protected abstract boolean containsAll(V who,V what);

    public boolean containsAll(QuickMap<K,V,?> set) {
        if(size>set.size) return false; // если больше то содержать не может

        for(int i=0;i<size;i++) {
            V inSet = set.get((K) table[indexes[i]],htable[indexes[i]]);
            if(inSet==null || !(containsAll(vtable[indexes[i]],inSet))) return false;
        }
        return true;
    }


    private V get(K key,int hash) {
        for(int i=hash & (table.length-1);table[i]!=null;i=(i==table.length-1?0:i+1))
            if(htable[i]==hash && table[i].equals(key))
                return vtable[i];
        return null;
    }

    public V get(K key) {
        return get(key,hash(key.hashCode()));
    }

    @Override
    public String toString() {
        String result = "";
        for(int i=0;i<size;i++)
            result = (result.length()==0?"":result+",") + table[indexes[i]] + " - " + vtable[indexes[i]];
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this==obj) return true;
        if(getClass()!=obj.getClass()) return false;

        QuickMap map = (QuickMap)obj;
        if(map.size!=size) return false;

        for(int i=0;i<size;i++) {
            Object mapValue = map.get(table[indexes[i]],htable[indexes[i]]);
            if(mapValue==null || !mapValue.equals(vtable[indexes[i]])) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for(int i=0;i<size;i++)
            hash = hash + htable[indexes[i]] ^ vtable[indexes[i]].hashCode();
        return hash;
    }
}
