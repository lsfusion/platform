package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.stored.StoredTestDataGenerators.StoredClass;
import org.junit.Test;

import static lsfusion.base.col.implementations.stored.StoredTestDataGenerators.sortedArray;
import static org.junit.Assert.*;

public class StoredArIndexedSetTest {
    @Test
    public void createWithArray() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = sortedArray();
        StoredArIndexedSet<StoredClass> result = new StoredArIndexedSet<>(arr, StoredArrayTest.serializer);
        assertEquals(arr.length, result.size());
        for (int i = 0; i < arr.length; ++i) {
            assertEquals(arr[i], result.get(i)); 
        }
        for (int i = 1; i < arr.length; ++i) {
            assertTrue(result.get(i-1).hashCode() <= result.get(i).hashCode());
        }
    }

    @Test
    public void createWithStoredArray() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = sortedArray();
        StoredArIndexedSet<StoredClass> result = initSet(arr);
        assertEquals(arr.length, result.size());
        for (int i = 0; i < arr.length; ++i) {
            assertEquals(arr[i], result.get(i));
        }
        for (int i = 1; i < arr.length; ++i) {
            assertTrue(result.get(i-1).hashCode() <= result.get(i).hashCode());
        }
    }

    @Test
    public void createWithStoredArIndexedSet() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = sortedArray();
        StoredArIndexedSet<StoredClass> result = initSet(arr);
        StoredArIndexedSet<StoredClass> copy = new StoredArIndexedSet<>(result);
        assertEquals(copy.size(), result.size());
        for (int i = 0; i < arr.length; ++i) {
            assertEquals(result.get(i), copy.get(i));
        }
    }

    @Test
    public void contains() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = sortedArray();
        StoredClass other = new StoredClass("otner", 6, true);
        StoredArIndexedSet<StoredClass> result = initSet(arr);
        for (int i = 0; i < arr.length; ++i) {
            assertTrue(result.contains(arr[i]));
        }
        assertFalse(result.contains(other));
        for (int i = arr.length - 1; i >= 0; --i) {
            assertTrue(result.contains(arr[i]));
        }
    }
    
    private StoredArIndexedSet<StoredClass> initSet(StoredClass[] arr) throws StoredArray.StoredArrayCreationException {
        StoredArray<StoredClass> stored = new StoredArray<>(arr, StoredArrayTest.serializer);
        StoredArIndexedSet<StoredClass> result = new StoredArIndexedSet<>(stored);
        return result;
    }

}