package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.*;
import lsfusion.base.col.implementations.order.*;
import lsfusion.base.col.implementations.stored.StoredTestDataGenerators.StoredClass;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;

import static org.junit.Assert.assertEquals;

public class SetsSerializationTest {
    static {
        StoredArrayTest.serializer.register(ArCol.class, ArCol::serialize, ArCol::deserialize);
        StoredArrayTest.serializer.register(ArList.class, ArList::serialize, ArList::deserialize);
        StoredArrayTest.serializer.register(ArSet.class, ArSet::serialize, ArSet::deserialize);
        StoredArrayTest.serializer.register(HSet.class, HSet::serialize, HSet::deserialize);
        
        StoredArrayTest.serializer.register(ArIndexedSet.class, ArIndexedSet::serialize, ArIndexedSet::deserialize);
        StoredArrayTest.serializer.register(ArOrderSet.class, ArOrderSet::serialize, ArOrderSet::deserialize);
        StoredArrayTest.serializer.register(HOrderSet.class, HOrderSet::serialize, HOrderSet::deserialize);
//        StoredArrayTest.serializer.register(ArOrderIndexedSet.class, ArOrderIndexedSet::serialize, ArOrderIndexedSet::deserialize);
    }
    
    @Test
    public void createAndSerializeArCol() {
        StoredArray<ArCol<StoredClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        StoredClass[] array = StoredTestDataGenerators.arrayWithNulls();
        ArCol<StoredClass> first = new ArCol<>();
        for (int i = 0; i < array.length / 2; ++i) {
            first.add(array[i]);
        }

        ArCol<StoredClass> second = new ArCol<>();
        for (int i = array.length / 2; i < array.length; ++i) {
            second.add(array[i]);
        }
        
        stored.append(first);
        stored.append(second);
        
        checkDeserializedEquality(array, stored);        
    }

    @Test
    public void createAndSerializeArList() {
        StoredArray<ArList<StoredClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        StoredClass[] array = StoredTestDataGenerators.arrayWithNulls();
        ArList<StoredClass> first = new ArList<>();
        for (int i = 0; i < array.length / 2; ++i) {
            first.add(array[i]);
        }

        ArList<StoredClass> second = new ArList<>();
        for (int i = array.length / 2; i < array.length; ++i) {
            second.add(array[i]);
        }

        stored.append(first);
        stored.append(second);

        ArList<StoredClass> first1 = stored.get(0);
        ArList<StoredClass> second1 = stored.get(1);

        assertEquals(first1.size(), array.length / 2);
        assertEquals(second1.size(), array.length - array.length / 2);

        for (int i = 0; i < array.length / 2; ++i) {
            assertEquals(array[i], first1.get(i));
        }
        for (int i = array.length / 2; i < array.length; ++i) {
            assertEquals(array[i], second1.get(i - array.length / 2));
        }
    }
    
    @Test
    public void createAndSerializeArSet() {
        StoredArray<ArSet<StoredClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        ArSet<StoredClass> first = new ArSet<>(array.length / 2);
        ArSet<StoredClass> second = new ArSet<>(array.length - array.length / 2);
        fillMSets(array, first, second, stored);
        checkDeserializedEquality(array, stored);
    }

    @Test
    public void createAndSerializeHSet() {
        StoredArray<HSet<StoredClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        HSet<StoredClass> first = new HSet<>();
        HSet<StoredClass> second = new HSet<>();
        fillMSets(array, first, second, stored);
        checkDeserializedEquality(array, stored);
    }

    @Test
    public void createAndSerializeArIndexedSet() {
        StoredArray<ArIndexedSet<StoredClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        array = Arrays.stream(array).sorted(Comparator.comparingInt(StoredClass::hashCode)).toArray(StoredClass[]::new);
        StoredClass[] firstArray = Arrays.copyOfRange(array, 0, array.length / 2);
        StoredClass[] secondArray = Arrays.copyOfRange(array, array.length / 2, array.length);
        
        ArIndexedSet<StoredClass> first = new ArIndexedSet<>(firstArray.length, firstArray);
        ArIndexedSet<StoredClass> second = new ArIndexedSet<>(secondArray.length, secondArray);
        stored.append(first);
        stored.append(second);
        checkDeserializedEquality(array, stored);
    }

    @Test
    public void createAndSerializeArOrderSet() {
        StoredArray<ArOrderSet<StoredClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        ArOrderSet<StoredClass> first = new ArOrderSet<>(array.length / 2);
        ArOrderSet<StoredClass> second = new ArOrderSet<>(array.length - array.length / 2);
        fillMOrderSets(array, first, second, stored);
        checkDeserializedEqualityOrdered(array, stored);
    }

    @Test
    public void createAndSerializeHOrderSet() {
        StoredArray<HOrderSet<StoredClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        HOrderSet<StoredClass> first = new HOrderSet<>();
        HOrderSet<StoredClass> second = new HOrderSet<>();
        fillMOrderSets(array, first, second, stored);
        checkDeserializedEqualityOrdered(array, stored);
    }

    private <T extends MSet<StoredClass>> void fillMSets(StoredClass[] array, T first, T second, StoredArray<T> stored) {
        for (int i = 0; i < array.length / 2; ++i) {
            first.add(array[i]);
        }

        for (int i = array.length / 2; i < array.length; ++i) {
            second.add(array[i]);
        }

        stored.append(first);
        stored.append(second);
    }
    
    private <T extends MOrderSet<StoredClass>> void fillMOrderSets(StoredClass[] array, T first, T second, StoredArray<T> stored) {
        for (int i = 0; i < array.length / 2; ++i) {
            first.add(array[i]);
        }

        for (int i = array.length / 2; i < array.length; ++i) {
            second.add(array[i]);
        }

        stored.append(first);
        stored.append(second);
    }
    
    private void checkDeserializedEquality(StoredClass[] array, StoredArray<? extends ImCol<StoredClass>> stored) {
        ImCol<StoredClass> first = stored.get(0);
        ImCol<StoredClass> second = stored.get(1);

        assertEquals(first.size(), array.length / 2);
        assertEquals(second.size(), array.length - array.length / 2);

        for (int i = 0; i < array.length / 2; ++i) {
            assertEquals(array[i], first.get(i));
        }
        for (int i = array.length / 2; i < array.length; ++i) {
            assertEquals(array[i], second.get(i - array.length / 2));
        }
    }

    private void checkDeserializedEqualityOrdered(StoredClass[] array, StoredArray<? extends MOrderSet<StoredClass>> stored) {
        MOrderSet<StoredClass> first = stored.get(0);
        MOrderSet<StoredClass> second = stored.get(1);

        assertEquals(first.size(), array.length / 2);
        assertEquals(second.size(), array.length - array.length / 2);

        for (int i = 0; i < array.length / 2; ++i) {
            assertEquals(array[i], first.get(i));
        }
        for (int i = array.length / 2; i < array.length; ++i) {
            assertEquals(array[i], second.get(i - array.length / 2));
        }
    }    
}
