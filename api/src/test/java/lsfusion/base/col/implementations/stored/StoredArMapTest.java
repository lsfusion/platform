package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.stored.StoredTestDataGenerators.StoredClass;
import org.junit.Test;

import static lsfusion.base.col.implementations.stored.StoredTestDataGenerators.simpleArray;
import static lsfusion.base.col.implementations.stored.StoredTestDataGenerators.stringArray;
import static org.junit.Assert.assertEquals;

public class StoredArMapTest {
    @Test
    public void createWithArrays() throws StoredArray.StoredArrayCreationException {
        StoredClass[] keysArray = simpleArray();
        StoredClass[] valuesArray = simpleArray();
        StoredArMap<StoredClass, StoredClass> map =
                new StoredArMap<>(keysArray.length, keysArray, valuesArray, StoredArrayTest.serializer);
        checkEquality(map, keysArray, valuesArray);
    }

    @Test
    public void createWithArrays2() throws StoredArray.StoredArrayCreationException {
        StoredClass[] keysArray = simpleArray();
        String[] valuesArray = stringArray();
        StoredArMap<StoredClass, String> map =
                new StoredArMap<>(keysArray.length, keysArray, valuesArray, StoredArrayTest.serializer);
        checkEquality(map, keysArray, valuesArray);
    }
    

    @Test
    public void createWithStored() throws StoredArray.StoredArrayCreationException {
        StoredClass[] keysArray = simpleArray();
        String[] valuesArray = stringArray();
        StoredArray<StoredClass> keys = new StoredArray<>(keysArray, StoredArrayTest.serializer);
        StoredArray<String> values = new StoredArray<>(valuesArray, StoredArrayTest.serializer);
        StoredArMap<StoredClass, String> map = new StoredArMap<>(keys, values);
        checkEquality(map, keysArray, valuesArray);
    }

    @Test
    public void createWithExclAdd() {
        StoredClass[] keys = simpleArray();
        String[] values = stringArray();
        StoredArMap<StoredClass, String> map = new StoredArMap<>(
            new StoredArray<>(0, StoredArrayTest.serializer),
            new StoredArray<>(0, StoredArrayTest.serializer)
        );
        for (int i = 0; i < keys.length; ++i) {
            map.exclAdd(keys[i], values[i]);
        }
        checkEquality(map, keys, values);
    }

    @Test
    public void mapValue() throws StoredArray.StoredArrayCreationException {
        StoredClass[] keys = simpleArray();
        int n = keys.length;
        String[] values = stringArray();
        StoredArMap<StoredClass, String> map =
                new StoredArMap<>(n, keys, values, StoredArrayTest.serializer);
        for (int i = 0; i < n; ++i) {
            map.mapValue(i, values[n - i - 1]);
        }
        for (int i = 0; i < n; ++i) {
            assertEquals(keys[i], map.getKey(i));
            assertEquals(values[i], map.getValue(n-i-1));
        }
    }

    @Test
    public void getObject() throws StoredArray.StoredArrayCreationException {
        StoredClass[] keys = simpleArray();
        StoredClass[] values = simpleArray();
        StoredArMap<StoredClass, StoredClass> map =
                new StoredArMap<>(keys.length, keys, values, StoredArrayTest.serializer);
        for (int i = 0; i < keys.length; ++i) {
            assertEquals(values[i], map.getObject(keys[i]));
        }
    }

    private void checkEquality(StoredArMap<?, ?> map, Object[] keys, Object[] values) {
        assertEquals(keys.length, map.size());
        for (int i = 0; i < keys.length; ++i) {
            assertEquals(keys[i], map.getKey(i));
            assertEquals(values[i], map.getValue(i));
        }
    }
}
