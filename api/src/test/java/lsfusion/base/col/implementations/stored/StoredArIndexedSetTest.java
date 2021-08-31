package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.stored.StoredArrayTest.SerializableClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;

import static org.junit.Assert.*;

public class StoredArIndexedSetTest {
    @Test
    public void createWithArray() {
        SerializableClass[] arr = initSortedArray();
        StoredArIndexedSet<SerializableClass> result = new StoredArIndexedSet<>(StoredArrayTest.serializer, arr);
        assertEquals(arr.length, result.size());
        for (int i = 0; i < arr.length; ++i) {
            assertEquals(arr[i], result.get(i)); 
        }
        for (int i = 1; i < arr.length; ++i) {
            assertTrue(result.get(i-1).hashCode() <= result.get(i).hashCode());
        }
    }

    @Test
    public void createWithStoredArray() {
        SerializableClass[] arr = initSortedArray();
        StoredArIndexedSet<SerializableClass> result = initSet(arr);
        assertEquals(arr.length, result.size());
        for (int i = 0; i < arr.length; ++i) {
            assertEquals(arr[i], result.get(i));
        }
        for (int i = 1; i < arr.length; ++i) {
            assertTrue(result.get(i-1).hashCode() <= result.get(i).hashCode());
        }
    }

    @Test
    public void createWithStoredArIndexedSet() {
        SerializableClass[] arr = initSortedArray();
        StoredArIndexedSet<SerializableClass> result = initSet(arr);
        StoredArIndexedSet<SerializableClass> copy = new StoredArIndexedSet<>(result);
        assertEquals(copy.size(), result.size());
        for (int i = 0; i < arr.length; ++i) {
            assertNotSame(result.get(i), copy.get(i));
            assertEquals(result.get(i), copy.get(i));
        }
    }


    @Test
    public void contains() {
        SerializableClass[] arr = initSortedArray();
        SerializableClass other = new SerializableClass("otner", 6, true);
        StoredArIndexedSet<SerializableClass> result = initSet(arr);
        for (int i = 0; i < arr.length; ++i) {
            assertTrue(result.contains(arr[i]));
        }
        assertFalse(result.contains(other));
        for (int i = arr.length - 1; i >= 0; --i) {
            assertTrue(result.contains(arr[i]));
        }
    }
    
    private SerializableClass[] initSortedArray() {
        SerializableClass[] array = StoredArrayTest.initArray();
        Arrays.sort(array, Comparator.comparingInt(SerializableClass::hashCode));
        return array;
    }
    
    private StoredArIndexedSet<SerializableClass> initSet(SerializableClass[] arr) {
        StoredArray<SerializableClass> stored = new StoredArray<>(arr, StoredArrayTest.serializer);
        StoredArIndexedSet<SerializableClass> result = new StoredArIndexedSet<>(stored);
        return result;
    }

}