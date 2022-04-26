package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.ArIndexedMap;
import lsfusion.base.col.implementations.ArIndexedSet;
import lsfusion.base.col.implementations.stored.StoredTestDataGenerators.DerivedStoredClass;
import lsfusion.base.col.implementations.stored.StoredTestDataGenerators.StoredClass;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import org.junit.Test;

import static lsfusion.base.col.implementations.stored.StoredTestDataGenerators.largeSortedArray;
import static lsfusion.base.col.implementations.stored.StoredTestDataGenerators.largeStringArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArIndexedTest {
    static {
        StoredArraySerializerRegistry serializer = (StoredArraySerializerRegistry) StoredArraySerializer.getInstance();
        serializer.register(StoredClass.class, StoredClass::serialize, StoredClass::deserialize);
        serializer.register(DerivedStoredClass.class, DerivedStoredClass::serialize, DerivedStoredClass::deserialize);
        System.setProperty("storedArraysEnabled", "true");
    }

    @Test
    public void checkArIndexedSetKeepTransformation() {
        StoredClass[] arr = largeSortedArray();
        ArIndexedSet<StoredClass> set = new ArIndexedSet<>(arr.length);
        for (StoredClass cls : arr) {
            set.keep(cls);
        }
        
        assertTrue(set.isStored());
        assertEquals(set.size(), arr.length); 
        for (int i = arr.length - 1; i >= 0; --i) {
            assertEquals(arr[i], set.get(i));
        }
    }

    @Test
    public void checkArIndexedSetImmutableTransformation() {
        StoredClass[] arr = largeSortedArray();
        ArIndexedSet<StoredClass> set = new ArIndexedSet<>(arr.length, arr);
        ImSet<StoredClass> storedSet = set.immutable();   

        assertTrue(storedSet instanceof StoredArIndexedSet);
        assertEquals(storedSet.size(), set.size());
        for (int i = set.size()-1; i >= 0; --i) {
            assertEquals(storedSet.get(i), set.get(i));
        }
    }
    
    @Test
    public void checkArIndexedSetConstructorTransformation() {
        StoredClass[] arr = largeSortedArray();
        ArIndexedSet<StoredClass> set = new ArIndexedSet<>(arr.length, arr);
        ArIndexedSet<StoredClass> targetSet = new ArIndexedSet<>(set);

        assertTrue(targetSet.isStored());
        assertEquals(targetSet.size(), set.size());
        for (int i = set.size() - 1; i >= 0; --i) {
            assertEquals(targetSet.get(i), set.get(i));
        }
    }
    
    @Test
    public void checkArIndexedMapImmutableTransformation() {
        StoredClass[] keys = largeSortedArray();
        String[] values = largeStringArray();
        ArIndexedMap<StoredClass, String> map = new ArIndexedMap<>(keys.length, keys, values);
        
        ImMap<StoredClass, String> storedMap = map.immutable();

        assertTrue(storedMap instanceof StoredArIndexedMap);
        assertEquals(storedMap.size(), map.size());
        for (int i = 0; i < map.size(); ++i) {
            assertEquals(storedMap.getKey(i), map.getKey(i));
            assertEquals(storedMap.getValue(i), map.getValue(i));
        }
        for (int i = map.size()-1; i >= 0; --i) {
            assertEquals(storedMap.get(keys[i]), map.get(keys[i]));
        }
    }

    @Test
    public void checkArIndexedMapConstructorTransformation() {
        StoredClass[] keys = largeSortedArray();
        String[] values = largeStringArray();
        ArIndexedMap<StoredClass, String> map = new ArIndexedMap<>(keys.length, keys, values);
        ArIndexedMap<StoredClass, String> storedMap = new ArIndexedMap<>(map, true);

        assertTrue(storedMap.isStored());
        assertEquals(storedMap.size(), keys.length);
        for (int i = 0; i < storedMap.size(); ++i) {
            assertEquals(storedMap.getKey(i), keys[i]);
            assertEquals(storedMap.getValue(i), values[i]);
        }
    }
}
