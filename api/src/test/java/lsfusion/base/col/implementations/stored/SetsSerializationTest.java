package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.*;
import lsfusion.base.col.implementations.order.*;
import lsfusion.base.col.implementations.stored.StoredArrayTest.SerializableClass;
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
        StoredArray<ArCol<SerializableClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        SerializableClass[] array = StoredArrayTest.initArrayWithNulls();
        ArCol<SerializableClass> first = new ArCol<>();
        for (int i = 0; i < array.length / 2; ++i) {
            first.add(array[i]);
        }

        ArCol<SerializableClass> second = new ArCol<>();
        for (int i = array.length / 2; i < array.length; ++i) {
            second.add(array[i]);
        }
        
        stored.append(first);
        stored.append(second);
        
        checkDeserializedEquality(array, stored);        
    }

    @Test
    public void createAndSerializeArList() {
        StoredArray<ArList<SerializableClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        SerializableClass[] array = StoredArrayTest.initArrayWithNulls();
        ArList<SerializableClass> first = new ArList<>();
        for (int i = 0; i < array.length / 2; ++i) {
            first.add(array[i]);
        }

        ArList<SerializableClass> second = new ArList<>();
        for (int i = array.length / 2; i < array.length; ++i) {
            second.add(array[i]);
        }

        stored.append(first);
        stored.append(second);

        ArList<SerializableClass> first1 = stored.get(0);
        ArList<SerializableClass> second1 = stored.get(1);

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
        StoredArray<ArSet<SerializableClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        SerializableClass[] array = StoredArrayTest.initArray();
        ArSet<SerializableClass> first = new ArSet<>(array.length / 2);
        ArSet<SerializableClass> second = new ArSet<>(array.length - array.length / 2);
        fillMSets(array, first, second, stored);
        checkDeserializedEquality(array, stored);
    }

    @Test
    public void createAndSerializeHSet() {
        StoredArray<HSet<SerializableClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        SerializableClass[] array = StoredArrayTest.initArray();
        HSet<SerializableClass> first = new HSet<>();
        HSet<SerializableClass> second = new HSet<>();
        fillMSets(array, first, second, stored);
        checkDeserializedEquality(array, stored);
    }

    @Test
    public void createAndSerializeArIndexedSet() {
        StoredArray<ArIndexedSet<SerializableClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        SerializableClass[] array = StoredArrayTest.initArray();
        array = Arrays.stream(array).sorted(Comparator.comparingInt(SerializableClass::hashCode)).toArray(SerializableClass[]::new);
        SerializableClass[] firstArray = Arrays.copyOfRange(array, 0, array.length / 2);
        SerializableClass[] secondArray = Arrays.copyOfRange(array, array.length / 2, array.length);
        
        ArIndexedSet<SerializableClass> first = new ArIndexedSet<>(firstArray.length, firstArray);
        ArIndexedSet<SerializableClass> second = new ArIndexedSet<>(secondArray.length, secondArray);
        stored.append(first);
        stored.append(second);
        checkDeserializedEquality(array, stored);
    }

    @Test
    public void createAndSerializeArOrderSet() {
        StoredArray<ArOrderSet<SerializableClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        SerializableClass[] array = StoredArrayTest.initArray();
        ArOrderSet<SerializableClass> first = new ArOrderSet<>(array.length / 2);
        ArOrderSet<SerializableClass> second = new ArOrderSet<>(array.length - array.length / 2);
        fillMOrderSets(array, first, second, stored);
        checkDeserializedEqualityOrdered(array, stored);
    }

    @Test
    public void createAndSerializeHOrderSet() {
        StoredArray<HOrderSet<SerializableClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        SerializableClass[] array = StoredArrayTest.initArray();
        HOrderSet<SerializableClass> first = new HOrderSet<>();
        HOrderSet<SerializableClass> second = new HOrderSet<>();
        fillMOrderSets(array, first, second, stored);
        checkDeserializedEqualityOrdered(array, stored);
    }

    private <T extends MSet<SerializableClass>> void fillMSets(SerializableClass[] array, T first, T second, StoredArray<T> stored) {
        for (int i = 0; i < array.length / 2; ++i) {
            first.add(array[i]);
        }

        for (int i = array.length / 2; i < array.length; ++i) {
            second.add(array[i]);
        }

        stored.append(first);
        stored.append(second);
    }
    
    private <T extends MOrderSet<SerializableClass>> void fillMOrderSets(SerializableClass[] array, T first, T second, StoredArray<T> stored) {
        for (int i = 0; i < array.length / 2; ++i) {
            first.add(array[i]);
        }

        for (int i = array.length / 2; i < array.length; ++i) {
            second.add(array[i]);
        }

        stored.append(first);
        stored.append(second);
    }
    
    private void checkDeserializedEquality(SerializableClass[] array, StoredArray<? extends ImCol<SerializableClass>> stored) {
        ImCol<SerializableClass> first = stored.get(0);
        ImCol<SerializableClass> second = stored.get(1);

        assertEquals(first.size(), array.length / 2);
        assertEquals(second.size(), array.length - array.length / 2);

        for (int i = 0; i < array.length / 2; ++i) {
            assertEquals(array[i], first.get(i));
        }
        for (int i = array.length / 2; i < array.length; ++i) {
            assertEquals(array[i], second.get(i - array.length / 2));
        }
    }

    private void checkDeserializedEqualityOrdered(SerializableClass[] array, StoredArray<? extends MOrderSet<SerializableClass>> stored) {
        MOrderSet<SerializableClass> first = stored.get(0);
        MOrderSet<SerializableClass> second = stored.get(1);

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
