package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.HSet;
import org.junit.Test;

import java.util.Arrays;

import static lsfusion.base.col.implementations.stored.StoredTestDataGenerators.*;
import static org.junit.Assert.*;

public class StoredHSetTest {
    @Test
    public void simpleInit() throws StoredArray.StoredArrayCreationException {
        StoredClass[] array = simpleArray();
        StoredHSet<StoredClass> storedSet = initSet(array);
        checkEquality(storedSet, array);
    }

    @Test
    public void createWithClone() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = simpleArray();
        StoredHSet<StoredClass> set = initSet(arr);
        StoredHSet<StoredClass> copySet = new StoredHSet<>(set);
        StoredClass added = new StoredClass("new element", 0, true);
        copySet.add(added);
        assertEquals(copySet.size(), set.size() + 1);
        for (int i = 0; i < arr.length; ++i) {
            assertEquals(arr[i], copySet.get(i));
        }
        assertEquals(copySet.get(arr.length), added);
    }

    @Test
    public void createWithStoredArray() {
        StoredClass[] arr = simpleArray();
        HSet<StoredClass> set = new HSet<>();
        for (StoredClass obj : arr) {
            set.add(obj);
        }
        StoredArray<StoredClass> stored = new StoredArray<>(StoredArraySerializer.getInstance());
        Arrays.stream(set.getTable()).forEach(obj -> stored.append((StoredClass) obj));
        StoredHSet<StoredClass> storedSet = new StoredHSet<>(set.size(), stored, set.getIndexes());
        checkEquality(storedSet, arr);
    }


    @Test
    public void contains() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = simpleArray();
        StoredHSet<StoredClass> result = initSet(arr);
        for (StoredClass storedClass : arr) {
            assertTrue(result.contains(storedClass));
        }
        StoredClass other = new StoredClass("other", 6, true);
        assertFalse(result.contains(other));
        for (int i = arr.length - 1; i >= 0; --i) {
            assertTrue(result.contains(arr[i]));
        }
    }

    @Test(expected = AssertionError.class)
    public void getIdentIncl() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = simpleArray();
        StoredHSet<StoredClass> result = initSet(arr);
        for (StoredClass storedClass : arr) {
            assertEquals(result.getIdentIncl(storedClass), storedClass);
        }
        StoredClass other = new StoredClass("other", 6, true);
        assertNull(result.getIdentIncl(other));
        for (int i = arr.length - 1; i >= 0; --i) {
            assertEquals(result.getIdentIncl(arr[i]), arr[i]);
        }
    }

    @Test
    public void addEqual() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = simpleArray();
        StoredHSet<StoredClass> set = initSet(arr);
        for (StoredClass obj : arr) {
            assertTrue(set.add(obj));
        }
        assertEquals(arr.length, set.size());
    }

    @Test
    public void add() throws StoredArray.StoredArrayCreationException {
        StoredClass[] arr = simpleArray();
        StoredHSet<StoredClass> set = initSet(arr);
        StoredClass[] largeArr = mediumSimpleArray();
        for (StoredClass obj : largeArr) {
            obj.cnt += 1000;
            assertFalse(set.add(obj));
        }
        assertEquals(arr.length + largeArr.length, set.size());
    }

    private StoredHSet<StoredClass> initSet(StoredClass[] arr) throws StoredArray.StoredArrayCreationException {
        HSet<StoredClass> set = new HSet<>();
        for (StoredClass obj : arr) {
            set.add(obj);
        }
        Object[] table = set.getTable();
        StoredClass[] tmparr = Arrays.stream(table).map(obj -> (StoredClass)obj).toArray(StoredClass[]::new);
        return new StoredHSet<>(set.size(), tmparr, set.getIndexes(), StoredArrayTest.serializer);
    }

    private void checkEquality(StoredHSet<StoredClass> set, StoredClass[] array) {
        assertEquals(array.length, set.size());
        for (int i = 0; i < array.length; ++i) {
            assertEquals(array[i], set.get(i));
        }
    }
}
