package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.stored.StoredTestDataGenerators.StoredClass;
import org.junit.Test;

import static lsfusion.base.col.implementations.stored.StoredTestDataGenerators.*;
import static lsfusion.base.col.implementations.stored.StoredTestDataGenerators.sortedArray;
import static org.junit.Assert.assertEquals;

public class StoredArIndexedMapTest {
    @Test
    public void createWithArrays() {
        StoredClass[] keysArray = sortedArray();
        StoredClass[] valuesArray = simpleArray();
        StoredArIndexedMap<StoredClass, StoredClass> map =
                new StoredArIndexedMap<>(keysArray.length, keysArray, valuesArray, StoredArrayTest.serializer);
        checkEquality(map, keysArray, valuesArray);
    }

    @Test
    public void createWithStored() {
        StoredClass[] keysArray = sortedArray();
        StoredClass[] valuesArray = simpleArray();
        StoredArray<StoredClass> keys = new StoredArray<>(keysArray, StoredArrayTest.serializer);
        StoredArray<StoredClass> values = new StoredArray<>(valuesArray, StoredArrayTest.serializer);
        StoredArIndexedMap<StoredClass, StoredClass> map = new StoredArIndexedMap<>(keys, values);
        checkEquality(map, keysArray, valuesArray);
    }

    @Test
    public void createWithAdd() {
        StoredClass[] keys = sortedArray();
        StoredClass[] values = simpleArray();
        StoredArIndexedMap<StoredClass, StoredClass> map = 
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
        StoredClass[] keys = sortedArray();
        int n = keys.length;
        String[] values = stringArray();
        StoredArIndexedMap<StoredClass, String> map =
                new StoredArIndexedMap<>(n, keys, values, StoredArrayTest.serializer);
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
        StoredClass[] keys = sortedArray();
        StoredClass[] values = simpleArray();
        StoredArIndexedMap<StoredClass, StoredClass> map =
                new StoredArIndexedMap<>(keys.length, keys, values, StoredArrayTest.serializer);
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
}
