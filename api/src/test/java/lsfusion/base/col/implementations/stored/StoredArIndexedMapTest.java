package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.stored.StoredArrayTest.SerializableClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;

import static org.junit.Assert.assertEquals;

public class StoredArIndexedMapTest {
    @Test
    public void createWithArrays() {
        SerializableClass[] keysArray = initSortedArray();
        SerializableClass[] valuesArray = StoredArrayTest.initArray();
        StoredArIndexedMap<SerializableClass, SerializableClass> map =
                new StoredArIndexedMap<>(StoredArrayTest.serializer, keysArray.length, keysArray, valuesArray);
        checkEquality(map, keysArray, valuesArray);
    }

    @Test
    public void createWithStored() {
        SerializableClass[] keysArray = initSortedArray();
        SerializableClass[] valuesArray = StoredArrayTest.initArray();
        StoredArray<SerializableClass> keys = new StoredArray<>(keysArray, StoredArrayTest.serializer);
        StoredArray<SerializableClass> values = new StoredArray<>(valuesArray, StoredArrayTest.serializer);
        StoredArIndexedMap<SerializableClass, SerializableClass> map = new StoredArIndexedMap<>(keys, values);
        checkEquality(map, keysArray, valuesArray);
    }

    @Test
    public void createWithAdd() {
        SerializableClass[] keys = initSortedArray();
        SerializableClass[] values = StoredArrayTest.initArray();
        StoredArIndexedMap<SerializableClass, SerializableClass> map = 
                new StoredArIndexedMap<>(
                        new StoredArray<>(0, StoredArrayTest.serializer),
                        new StoredArray<>(0, StoredArrayTest.serializer)
                );
        for (int i = 0; i < keys.length; ++i) {
            map.add(keys[i], values[i]);
        }
        checkEquality(map, keys, values);
    }
    
    @Test
    public void mapValue() {
        SerializableClass[] keys = initSortedArray();
        int n = keys.length;
        String[] strings = StoredArrayTest.initStringArray();
        String[] values = new String[n];
        for (int i = 0; i < n; ++i) {
            values[i] = strings[i];
        }
        StoredArIndexedMap<SerializableClass, String> map =
                new StoredArIndexedMap<>(StoredArrayTest.serializer, n, keys, values);
        for (int i = 0; i < n; ++i) {
            map.mapValue(i, values[n - i - 1]);
        }
        for (int i = 0; i < n; ++i) {
            assertEquals(keys[i], map.getKey(i));
            assertEquals(values[i], map.getValue(n-i-1));
        }
    }

    @Test
    public void getObject() {
        SerializableClass[] keys = initSortedArray();
        SerializableClass[] values = StoredArrayTest.initArray();
        StoredArIndexedMap<SerializableClass, SerializableClass> map =
                new StoredArIndexedMap<>(StoredArrayTest.serializer, keys.length, keys, values);
        for (int i = 0; i < keys.length; ++i) {
            assertEquals(values[i], map.getObject(keys[i]));
        }
    }
    
    private void checkEquality(StoredArIndexedMap<?, ?> map, Object[] keys, Object[] values) {
        assertEquals(keys.length, map.size());
        for (int i = 0; i < keys.length; ++i) {
            assertEquals(keys[i], map.getKey(i));
            assertEquals(values[i], map.getValue(i));
        }
    }

    private StoredArIndexedMap<SerializableClass, SerializableClass> initMap() {
        SerializableClass[] keysArray = initSortedArray();
        SerializableClass[] valuesArray = StoredArrayTest.initArray();
        StoredArray<SerializableClass> keys = new StoredArray<>(keysArray, StoredArrayTest.serializer);
        StoredArray<SerializableClass> values = new StoredArray<>(valuesArray, StoredArrayTest.serializer);
        return new StoredArIndexedMap<>(keys, values);
    }

    private SerializableClass[] initSortedArray() {
        SerializableClass[] array = StoredArrayTest.initArray();
        Arrays.sort(array, Comparator.comparingInt(SerializableClass::hashCode));
        return array;
    }
}
