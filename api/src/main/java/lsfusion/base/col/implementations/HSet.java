package lsfusion.base.col.implementations;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AMSet;
import lsfusion.base.col.implementations.order.HOrderSet;
import lsfusion.base.col.implementations.stored.StoredArray;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.implementations.stored.StoredHSet;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static lsfusion.base.col.implementations.stored.StoredArray.isStoredArraysEnabled;
import static lsfusion.base.col.implementations.stored.StoredImplementationsPolicy.LIMIT;
import static lsfusion.base.col.implementations.stored.StoredImplementationsPolicy.STORED_FLAG;

public class HSet<T> extends AMSet<T> {
    private int size;
    private Object[] table;

    private int[] indexes;

    public static final float loadFactor = 0.3f;

    public HSet() {
        table = new Object[8];

        indexes = new int[(int)(table.length * loadFactor)];
    }

    public HSet(int size, Object[] table, int[] indexes) {
        this.size = size;
        this.table = table;
        this.indexes = indexes;
    }

    public HSet(int size, StoredArray<T> table, int[] indexes) {
        switchToStored(new StoredHSet<>(size, table, indexes));
    }

    public HSet(int size) {
        int initialCapacity = (int)(size/loadFactor) + 1;

        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;

        table = new Object[capacity];

        indexes = new int[size];
    }

    public HSet(HSet<T> set) {
        if (set.isStored()) {
            StoredHSet<T> storedSet = new StoredHSet<>(set.stored());
            switchToStored(storedSet);
        } else if (needToBeStored(set)) {
            switchToStored(set.size, set.table, set.indexes);
        }
        if (!isStored()) {
            size = set.size;
            table = set.table.clone();
            indexes = set.indexes.clone();
        }
    }

    public int size() {
        if (!isStored()) {
            return size;
        } else {
            return stored().size();
        }
    }

    public boolean contains(T where) {
        if (!isStored()) {
            // копися с hashSet'а
            for (int i = MapFact.colHash(where.hashCode()) & (table.length - 1); table[i] != null; i = (i == table.length - 1 ? 0 : i + 1))
                if (BaseUtils.hashEquals(table[i], where))
                    return true;
            return false;
        } else {
            return stored().contains(where);
        }
    }

    @Override
    public T getIdentIncl(T element) {
        if (!isStored()) {
            for (int i = MapFact.colHash(element.hashCode()) & (table.length - 1); table[i] != null; i = (i == table.length - 1 ? 0 : i + 1))
                if (BaseUtils.hashEquals(table[i], element))
                    return (T) table[i];
            assert false;
            return null;
        } else {
            return stored().getIdentIncl(element);
        }
    }

    /*    public HSet(HSet<T>[] sets) {
        HSet<T> minSet = sets[0];
        for(int i=1;i<sets.length;i++)
            if(sets[i].size<minSet.size)
                minSet = sets[i];

        table = new Object[minSet.table.length];
        htable = new int[table.length];

        indexes = new int[(int)(table.length * loadFactor)];

        for(int i=0;i<minSet.size;i++) {
            T element = minSet.get(i); int hash = minSet.htable[minSet.indexes[i]];
            boolean all = true;
            for(HSet<T> set : sets)
                if(set!=minSet && !set.contains(element,hash)) {
                    all = false;
                    break;
                }
            if(all)
                add(element,hash);
        }
    }*/

    private void resize(int length) {
        int[] newIndexes = new int[(int)(length * loadFactor)+1];

        Object[] newTable = new Object[length];
        for(int i=0;i<size;i++) {
            Object object = table[indexes[i]];

            // копися с hashSet'а
            int newHash = MapFact.colHash(object.hashCode()) & (length-1);
            while(newTable[newHash]!=null) newHash = (newHash==length-1?0:newHash+1);
            newTable[newHash] = object;

            newIndexes[i] = newHash;
        }

        table = newTable;

        indexes = newIndexes;
    }

    public boolean add(T where) {
        if (!isStored()) {
            if (size >= indexes.length) resize(2 * table.length);
            // копися с hashSet'а
            int i = MapFact.colHash(where.hashCode()) & (table.length - 1);
            while (table[i] != null) {
                if (BaseUtils.hashEquals(table[i], where))
                    return true;
                i = (i == table.length - 1 ? 0 : i + 1);
            }
            table[i] = where;
            indexes[size++] = i;
            switchToStoredIfNeeded(size-1, size);
            return false;
        } else {
            return stored().add(where);
        }
    }

    public T get(int i) {
        if (!isStored()) {
            return (T) table[indexes[i]];
        } else {
            return stored().get(i);
        }
    }

    public <M> ImValueMap<T, M> mapItValues() {
        return new HMap<>(this);
    }

    public <M> ImRevValueMap<T, M> mapItRevValues() {
        return new HMap<>(this);
    }

    public ImSet<T> immutable() {
        if(size==0)
            return SetFact.EMPTY();
        if(size==1)
            return SetFact.singleton(single());

        if (needToBeStored(this) && switchToStored(size, table, indexes)) {
            return stored().immutable();
        }

        if (!isStored()) {
            if (size < SetFact.useArrayMax) {
                Object[] array = new Object[size];
                for (int i = 0; i < size; i++)
                    array[i] = get(i);
                return new ArSet<>(size, array);
            }

            if (size >= SetFact.useIndexedArrayMin) {
                Object[] array = new Object[size];
                for (int i = 0; i < size; i++)
                    array[i] = get(i);
                ArSet.sortArray(size, array);
                return new ArIndexedSet<>(size, array);
            }

            if (indexes.length > size * SetFact.factorNotResize) {
                int[] newIndexes = new int[size];
                System.arraycopy(indexes, 0, newIndexes, 0, size);
                indexes = newIndexes;
            }
        }
        return this;
    }

    public ImSet<T> immutableCopy() {
        return new HSet<>(this); // ?? may be new HSet<>(this).immutable() ?
    }

    @Override
    public HMap<T, T> toMap() {
        if (!isStored()) {
            return new HMap<>(size, table, table, indexes);
        } else {
            return new HMap<>(stored().size(), stored().getTable(), stored().getTable(), stored().getIndexes());
        }
    }

    @Override
    public ImRevMap<T, T> toRevMap() {
        return toMap();
    }

    @Override
    public ImOrderSet<T> toOrderSet() {
        return new HOrderSet<>(this);
    }

    public Object[] getTable() {
        if (!isStored()) {
            return table;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public int[] getIndexes() {
        if (!isStored()) {
            return indexes;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public void setIndexes(int[] indexes) {
        if (!isStored()) {
            this.indexes = indexes;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        HSet<?> set = (HSet<?>) o;
        serializer.serialize(set.size, outStream);
        ArCol.serializeArray(set.table, serializer, outStream);
        ArCol.serializeIntArray(set.indexes, serializer, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        int size = (int) serializer.deserialize(inStream);
        Object[] array = ArCol.deserializeArray(inStream, serializer);
        int[] indexes = ArCol.deserializeIntArray(inStream, serializer);
        return new HSet<>(size, array, indexes);
    }

    public boolean isStored() {
        return size == STORED_FLAG;
    }

    StoredHSet<T> stored() {
        return (StoredHSet<T>) table[0];
    }

    private void switchToStoredIfNeeded(int oldSize, int newSize) {
        if (oldSize <= LIMIT && newSize > LIMIT && needToBeStored(this)) {
            switchToStored(size, table, indexes);
        }
    }

    private static boolean needToBeStored(HSet<?> set) {
        return !set.isStored() && set.size() > LIMIT && canBeStored(set);
    }

    private static boolean canBeStored(HSet<?> set) {
        return isStoredArraysEnabled() && StoredArraySerializer.getInstance().canBeSerialized(set.get(0));
    }

    private boolean switchToStored(int size, Object[] array, int[] indexes) {
        try {
            StoredHSet<T> storedSet = new StoredHSet<>(size, (T[])array, indexes, StoredArraySerializer.getInstance());
            switchToStored(storedSet);
            return true;
        } catch (StoredArray.StoredArrayCreationException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void switchToStored(StoredHSet<T> storedSet) {
        this.table = new Object[]{storedSet};
        this.size = STORED_FLAG;
        this.indexes = null;
    }
}
