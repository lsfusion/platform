package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.stored.StoredTestDataGenerators.StoredClass;
import org.junit.Test;

import static lsfusion.base.col.implementations.stored.StoredTestDataGenerators.largeSimpleArray;
import static lsfusion.base.col.implementations.stored.StoredTestDataGenerators.simpleArray;
import static org.junit.Assert.*;

public class StoredArSetTest {
    @Test
    public void createWithArray() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = simpleArray();
        StoredArSet<StoredClass> set = new StoredArSet<>(arr, StoredArrayTest.serializer);
        checkEquality(set, arr);
    }

    @Test
    public void createWithStoredArray() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = simpleArray();
        StoredArSet<StoredClass> set =  initSet(arr);
        checkEquality(set, arr);
    }

    @Test
    public void createWithStoredArSet() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = simpleArray();
        StoredArSet<StoredClass> set = initSet(arr);
        StoredArSet<StoredClass> copy = new StoredArSet<>(set);
        checkEquality(copy, arr);
    }


    @Test
    public void contains() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = simpleArray();
        StoredArSet<StoredClass> result = initSet(arr);
        for (StoredClass storedClass : arr) {
            assertTrue(result.contains(storedClass));
        }
        StoredClass other = new StoredClass("other", 6, true);
        assertFalse(result.contains(other));
        for (int i = arr.length - 1; i >= 0; --i) {
            assertTrue(result.contains(arr[i]));
        }
    }
    
    @Test
    public void exclAdd() {
        StoredClass[] arr = simpleArray();
        StoredArSet<StoredClass> set = new StoredArSet<>(0, StoredArrayTest.serializer);
        for (StoredClass obj : arr) {
            set.exclAdd(obj);
        }
        checkEquality(set, arr);
    }
    
    @Test
    public void largeCreateRead() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = largeSimpleArray();
        StoredArSet<StoredClass> set = new StoredArSet<>(arr.length, arr, StoredArrayTest.serializer);
        checkEquality(set, arr);
    }

    @Test
    public void largeWriteRead() {
        StoredClass[] arr = largeSimpleArray();
        StoredArSet<StoredClass> set = new StoredArSet<>(0, StoredArrayTest.serializer);
        for (StoredClass obj : arr) {
            set.exclAdd(obj);
        }
        checkEquality(set, arr);
    }
    
    private StoredArSet<StoredClass> initSet(StoredClass[] arr) throws StoredArray.StoredArrayCreationException {
        StoredArray<StoredClass> stored = new StoredArray<>(arr, StoredArrayTest.serializer);
        return new StoredArSet<>(stored);
    }

    private void checkEquality(StoredArSet<StoredClass> set, StoredClass[] array) {
        assertEquals(array.length, set.size());
        for (int i = 0; i < array.length; ++i) {
            assertEquals(array[i], set.get(i));
        }
    }
}
