package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import org.junit.Test;

import java.util.Arrays;

import static lsfusion.base.col.implementations.stored.StoredTestDataGenerators.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StoredHMapTest {
    @Test
    public void createWithArrays() throws StoredArray.StoredArrayCreationException {
        StoredClass[] keys = simpleArray();
        String[] values = stringArray();
        StoredHMap<StoredClass, String> stored = initMap(keys, values);
        checkEquality(stored, keys, values);
    }

    @Test
    public void createWithCloning() throws StoredArray.StoredArrayCreationException {
        StoredClass[] keys = simpleArray();
        String[] values = stringArray();
        StoredHMap<StoredClass, String> stored = initMap(keys, values);
        StoredHMap<StoredClass, String> copy = new StoredHMap<>(stored, true);
        copy.add(new StoredClass("name", -50, false), "added");
        assertEquals(copy.size(), stored.size() + 1);
        for (int i = 0; i < stored.size(); ++i) {
            assertEquals(stored.getKey(i), copy.getKey(i));
            assertEquals(stored.getValue(i), copy.getValue(i));
        }
    }

    @Test
    public void createWithAdd() throws StoredArray.StoredArrayCreationException {
        StoredClass[] keys = simpleArray();
        String[] values = stringArray();
        StoredHMap<StoredClass, String> stored = initMap(keys, values);
        StoredClass[] addKeys = mediumSimpleArray();
        String[] addValues = mediumStringArray();
        for (int i = 0; i < addKeys.length; ++i) {
            addKeys[i].cnt += 500;
            stored.add(addKeys[i], addValues[i]);
        }
        assertEquals(stored.size(), keys.length + addKeys.length);
        for (int i = 0; i < keys.length; ++i) {
            assertEquals(keys[i], stored.getKey(i));
            assertEquals(values[i], stored.getValue(i));
        }
        for (int i = 0; i < addKeys.length; ++i) {
            assertEquals(addKeys[i], stored.getKey(i + keys.length));
            assertEquals(addValues[i], stored.getValue(i + keys.length));
        }
    }

    @Test
    public void addEqual() throws StoredArray.StoredArrayCreationException {
        StoredClass[] keys = simpleArray();
        String[] values = stringArray();
        StoredHMap<StoredClass, String> stored = initMap(keys, values);
        for (int i = 0; i < keys.length; ++i) {
            stored.add(keys[i], "test"+i);
        }
        assertEquals(stored.size(), keys.length);
        for (int i = 0; i < keys.length; ++i) {
            assertEquals(keys[i], stored.getKey(i));
            assertEquals("test"+i, stored.getValue(i));
        }
    }

    @Test
    public void mapValue() throws StoredArray.StoredArrayCreationException {
        StoredClass[] keys = simpleArray();
        int n = keys.length;
        String[] values = stringArray();
        StoredHMap<StoredClass, String> map = initMap(keys, values);
        for (int i = 0; i < n; ++i) {
            map.mapValue(i, values[n - i - 1]);
        }

        assertEquals(keys.length, map.size());
        for (int i = 0; i < n; ++i) {
            assertEquals(keys[i], map.getKey(i));
            assertEquals(values[i], map.getValue(n-i-1));
        }
    }

    @Test
    public void getObject() throws StoredArray.StoredArrayCreationException {
        StoredClass[] keys = simpleArray();
        String[] values = stringArray();
        StoredHMap<StoredClass, String> map = initMap(keys, values);
        for (int i = 0; i < keys.length; ++i) {
            assertEquals(values[i], map.getObject(keys[i]));
        }
        assertNull(map.getObject(new StoredClass("getObject() test", 1, true)));
    }

    private StoredHMap<StoredClass, String> initMap(StoredClass[] keys, String[] values) throws StoredArray.StoredArrayCreationException {
        assert keys.length == values.length;

        HMap<StoredClass, String> map = new HMap<>((AddValue<StoredClass, String>) null);
        for (int i = 0; i < keys.length; ++i) {
            map.add(keys[i], values[i]);
        }
        StoredClass[] tmpkeys = Arrays.stream(map.getTable()).map(obj -> (StoredClass)obj).toArray(StoredClass[]::new);
        String[] tmpvalues = Arrays.stream(map.getVTable()).map(obj -> (String)obj).toArray(String[]::new);
        return new StoredHMap<>(map.size(), tmpkeys, tmpvalues, map.getIndexes(), StoredArrayTest.serializer, changeValue);
    }

    private void checkEquality(StoredHMap<?, ?> map, Object[] keys, Object[] values) {
        assertEquals(keys.length, map.size());
        for (int i = 0; i < keys.length; ++i) {
            assertEquals(keys[i], map.getKey(i));
            assertEquals(values[i], map.getValue(i));
        }
    }

    private final static AddValue<StoredClass, String> changeValue = new SymmAddValue<StoredClass, String>() {
        public String addValue(StoredClass key, String prevValue, String newValue) {
            return newValue;
        }
    };
}
