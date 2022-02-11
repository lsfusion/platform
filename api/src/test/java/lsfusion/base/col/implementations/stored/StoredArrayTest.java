package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.stored.StoredTestDataGenerators.DerivedStoredClass;
import lsfusion.base.col.implementations.stored.StoredTestDataGenerators.StoredClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StoredArrayTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    
    public static StoredArraySerializerRegistry serializer = new StoredArraySerializerRegistry();
    static {
        serializer.register(StoredClass.class, StoredClass::serialize, StoredClass::deserialize);
        serializer.register(DerivedStoredClass.class, DerivedStoredClass::serialize, DerivedStoredClass::deserialize);
    }

    @Test
    public void createWithArray() {
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        StoredArray<StoredClass> stored = new StoredArray<>(array, serializer);
        assertEquals(stored.size(), array.length);
    }

    @Test
    public void createWithZeroLengthArray() {
        StoredArray<StoredClass> stored = new StoredArray<>(new StoredClass[0], serializer);
        assertEquals(stored.size(), 0);
    }

    @Test
    public void createWithSize() {
        StoredArray<StoredClass> stored = new StoredArray<>(10, serializer);
        assertEquals(stored.size(), 10);
    }

    @Test
    public void createEmpty() {
        StoredArray<StoredClass> stored = new StoredArray<>(serializer);
        assertEquals(stored.size(), 0);
    }

    @Test
    public void createWithArrayAndGet() {
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        StoredArray<StoredClass> stored = new StoredArray<>(array, serializer);
        checkEquality(array, stored);
    }

    @Test
    public void createWithArrayAndSize() {
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        final int size = 4;
        StoredArray<StoredClass> stored = new StoredArray<>(array, size, serializer, null);
        assertEquals(size, stored.size());
        for (int i = 0; i < size; ++i) {
            assertEquals(array[i], stored.get(i));
        }
    } 
    
    @Test
    public void createWithArrayAndGetReversed() {
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        StoredArray<StoredClass> stored = new StoredArray<>(array, serializer);
        for (int i = stored.size() - 1; i >= 0; --i) {
            assertEquals(array[i], stored.get(i));
        }
    }

    @Test
    public void createWithCopyConstructor() {
        StoredClass[] array = StoredTestDataGenerators.arrayWithNulls();
        StoredArray<StoredClass> stored = new StoredArray<>(array, serializer);
        StoredArray<StoredClass> target = new StoredArray<>(stored);
        checkEquality(array, target);
    }

    @Test
    public void checkArrayCopy() {
        StoredClass[] array = StoredTestDataGenerators.arrayWithNulls();
        StoredArray<StoredClass> source = new StoredArray<>(array, serializer);
        StoredArray<StoredClass> target = new StoredArray<>(source);
        source.set(0, new StoredClass("test", 50, false));
        assertNull(target.get(0));
    }

    @Test
    public void checkUnserializableObjectAssert() {
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        array[3] = new StoredTestDataGenerators.OtherStoredClass("triangle", 5, true);
        thrown.expect(RuntimeException.class);
        StoredArray<StoredClass> stored = new StoredArray<>(array, serializer);
        stored.squeeze(); // this should flush inner buffer
    }

    @Test
    public void addAndGet() {
        StoredArray<StoredClass> stored = new StoredArray<>(serializer);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        for (StoredClass element : array) {
            stored.append(element);
        }
        checkEquality(array, stored);
    }

    @Test
    public void addAndImmediateGet() {
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        StoredArray<StoredClass> stored = new StoredArray<>(serializer);
        for (int i = 0; i < stored.size(); ++i) {
            stored.append(array[i]);
            assertEquals(array[i], stored.get(i));
        }
    }

    @Test
    public void createAndReverseMultipleTimes() {
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        StoredArray<StoredClass> stored = new StoredArray<>(array, serializer);
        int n = stored.size();
        for (int step = 0; step < 10; ++step) {
            for (int i = 0; i < n / 2; ++i) {
                StoredClass a = stored.get(i);
                StoredClass b = stored.get(n - i - 1);
                stored.set(i, b);
                stored.set(n - i - 1, a);
            }
        }
        checkEquality(array, stored);
    }

    @Test
    public void randomAddWithIndex() {
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        StoredArray<StoredClass> stored = new StoredArray<>(new StoredClass[array.length], serializer);
        int n = stored.size();
        List<Integer> indexes = IntStream.range(0, n).boxed().collect(Collectors.toList());
        Collections.shuffle(indexes);
        for (int i = 0; i < n; ++i) {
            stored.set(indexes.get(i), array[indexes.get(i)]);
        }
        checkEquality(array, stored);
    }

    @Test
    public void createWithSizeAndCheckForNulls() {
        StoredArray<StoredClass> stored = new StoredArray<>(10, serializer);
        for (int i = 0; i < stored.size(); ++i) {
            StoredClass cls = stored.get(i);
            assertNull(cls);
        }
    }

    @Test
    public void createWithArrayWithNullsAndGet() {
        StoredClass[] array = StoredTestDataGenerators.arrayWithNulls();
        StoredArray<StoredClass> stored = new StoredArray<>(array, serializer);
        checkEquality(array, stored);
    }

    @Test
    public void appendWithNullsAndGet() {
        StoredClass[] array = StoredTestDataGenerators.arrayWithNulls();
        StoredArray<StoredClass> stored = new StoredArray<>(serializer);
        for (StoredClass obj : array) {
            stored.append(obj);
        }
        checkEquality(array, stored);
    }

    @Test
    public void setWithNullsAndGet() {
        StoredClass[] array = StoredTestDataGenerators.arrayWithNulls();
        StoredArray<StoredClass> stored = new StoredArray<>(array.length, serializer);
        for (int i = 0; i < array.length; ++i) {
            stored.set(i, array[i]);
        }
        checkEquality(array, stored);
    }

    @Test
    public void squeeze() {
        StoredClass[] array = StoredTestDataGenerators.arrayWithNulls();
        final DerivedStoredClass weighty = new DerivedStoredClass("LongStringStringString", 45, false, "                    ");
        StoredArray<StoredClass> stored = new StoredArray<>(array, serializer);
        for (int i = 0; i < stored.size(); ++i) {
            if (stored.get(i) == null) {
                stored.set(i, weighty);
            }
        }
        stored.squeeze();
        for (int i = 0; i < stored.size(); ++i) {
            if (array[i] == null) {
                assertEquals(weighty, stored.get(i));
            } else {
                assertEquals(array[i], stored.get(i));
            }
        }
    }

    @Test
    public void insertToBeginning() {
        StoredClass[] array = StoredTestDataGenerators.arrayWithNulls();
        int mid = array.length / 2;
        StoredArray<StoredClass> stored = new StoredArray<>(serializer);
        for (int i = mid; i < array.length; ++i) {
            stored.append(array[i]);
        }
        for (int i = 0; i < mid; ++i) {
            stored.insert(i, array[i]);
        }
        checkEquality(array, stored);
    }

    @Test
    public void insertToMiddle() {
        StoredClass[] array = StoredTestDataGenerators.arrayWithNulls();
        int mid = array.length / 4;
        StoredArray<StoredClass> stored = new StoredArray<>(serializer);
        for (int i = 0; i < mid; ++i) {
            stored.append(array[i]);
        }
        for (int i = array.length - mid; i < array.length; ++i) {
            stored.append(array[i]);
        }
        for (int i = array.length - mid - 1; i >= mid; --i) {
            stored.insert(mid, array[i]);
        }
        checkEquality(array, stored);
    }

    @Test
    public void insertToEnd() {
        StoredClass[] array = StoredTestDataGenerators.arrayWithNulls();
        StoredArray<StoredClass> stored = new StoredArray<>(serializer);
        for (int i = 0; i < array.length; ++i) {
            stored.insert(i, array[i]);
        }
        checkEquality(array, stored);
    }

    @Test
    public void stringArray() {
        String[] strings = StoredTestDataGenerators.stringArray();
        StoredArray<String> stored = new StoredArray<>(strings, serializer);
        checkEquality(strings, stored);
        strings[0] = "First";
        strings[7] = "longlonglonglonglong";
        stored.set(0, strings[0]);
        stored.set(7, strings[7]);
        checkEquality(strings, stored);
    }

    @Test
    public void mixedArray() {
        Object[] objects = StoredTestDataGenerators.mixedArray();
        StoredArray<Object> stored = new StoredArray<>(objects, serializer);
        checkEquality(objects, stored);
        objects[0] = 5.0;
        objects[7] = 5;
        stored.set(0, 5.0);
        stored.set(7, 5);
        checkEquality(objects, stored);
    }

    private void checkEquality(Object[] array, StoredArray<?> stored) {
        assertEquals(array.length, stored.size());
        for (int i = 0; i < stored.size(); ++i) {
            assertEquals(array[i], stored.get(i));
        }
    }

    @Test
    public void performanceTest() {
        StoredClass[] simpleArr = StoredTestDataGenerators.largeSimpleArray();
        StoredArray<StoredClass> simpleStored = new StoredArray<>(simpleArr, serializer);
        checkEquality(simpleArr, simpleStored);

        StoredClass[] withNullsArr = StoredTestDataGenerators.largeArrayWithNulls();
        StoredArray<StoredClass> storedWithNulls = new StoredArray<>(withNullsArr, serializer);
        checkEquality(withNullsArr, storedWithNulls);

        Object[] mixedArr = StoredTestDataGenerators.largeMixedArray();
        StoredArray<Object> mixedStored = new StoredArray<>(mixedArr, serializer);
        assertEquals(mixedArr.length, mixedStored.size());
        for (int i = 0; i < mixedStored.size(); ++i) {
            assertEquals(mixedArr[i], mixedStored.get(i));
        }
    }

}
