package lsfusion.base.col.implementations.stored;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.implementations.abs.AMSet;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

public class StoredHSet<T> extends AMSet<T> {
    private int size;
    private StoredArray<T> table;
    private int[] indexes;

    public static final float loadFactor = 0.3f;

    public StoredHSet(int size, T[] table, int[] indexes, StoredArraySerializer serializer) throws StoredArray.StoredArrayCreationException {
        this.size = size;
        this.indexes = indexes;
        this.table = new StoredArray<>(table, serializer);
    }

    public StoredHSet(StoredHSet<T> set) {
        size = set.size;
        table = new StoredArray<>(set.table);
        indexes = set.indexes.clone();
    }

    public StoredHSet(int size, StoredArray<T> table, int[] indexes) {
        this.size = size;
        this.indexes = indexes;
        this.table = table;
    }

    public int size() {
        return size;
    }

    public boolean contains(T element) {
        int start = MapFact.colHash(element.hashCode()) & (table.size()-1);
        for (int i = start; table.get(i) != null; i = (i == table.size()-1 ? 0 : i+1)) {
            if (BaseUtils.hashEquals(table.get(i), element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public T getIdentIncl(T element) {
        int start = MapFact.colHash(element.hashCode()) & (table.size()-1);
        for (int i = start; table.get(i) != null; i = (i == table.size()-1 ? 0 : i+1)) {
            if (BaseUtils.hashEquals(table.get(i), element)) {
                return table.get(i);
            }
        }
        assert false;
        return null;
    }

    private void resize(int length) {
        int[] newIndexes = new int[(int)(length * loadFactor)+1];
        StoredArray<T> newTable = new StoredArray<>(length, table.getSerializer());
        for (int i = 0; i < size; ++i) {
            T object = table.get(indexes[i]);
            int index = MapFact.colHash(object.hashCode()) & (length-1);
            while (newTable.get(index) != null) {
                index = (index == length-1 ? 0 : index+1);
            }
            newTable.set(index, object);
            newIndexes[i] = index;
        }

        table = newTable;
        indexes = newIndexes;
    }

    public boolean add(T element) {
        if (size >= indexes.length) resize(2 * table.size());

        int i = MapFact.colHash(element.hashCode()) & (table.size()-1);
        while (table.get(i) != null) {
            if (BaseUtils.hashEquals(table.get(i), element))
                return true;
            i = (i == table.size()-1 ? 0 : i+1);
        }
        table.set(i, element);
        indexes[size++] = i;
        return false;
    }

    public T get(int i) {
        return table.get(indexes[i]);
    }

    public <M> ImValueMap<T, M> mapItValues() {
        return new StoredHMap<>(this);
    }

    public <M> ImRevValueMap<T, M> mapItRevValues() {
        return new StoredHMap<>(this);
    }

    public ImSet<T> immutable() {
//        StoredArray<T> tmpArray = new StoredArray<>(table.getSerializer());
//        for(int i = 0; i < size; ++i) {
//            tmpArray.append(get(i));
//        }
//        tmpArray.sort();
//        return new StoredArIndexedSet<>(tmpArray);
        return this;
    }

    public ImSet<T> immutableCopy() {
        return new StoredHSet<>(this).immutable();
    }

    @Override
    public StoredHMap<T, T> toMap() {
        return new StoredHMap<>(size, table, table, indexes);
    }

    @Override
    public ImRevMap<T, T> toRevMap() {
        return toMap();
    }

    @Override
    public ImOrderSet<T> toOrderSet() {
        // todo [dale]: uncomment
        // return new StoredHOrderSet<>(this);
        return null;
    }

    public StoredArray<T> getTable() {
        return table;
    }

    public int[] getIndexes() {
        return indexes;
    }
}
