package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.ArIndexedMap;
import lsfusion.base.col.implementations.ArMap;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.implementations.order.ArOrderMap;
import lsfusion.base.col.implementations.order.HOrderMap;
import lsfusion.base.col.implementations.stored.StoredTestDataGenerators.StoredClass;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MOrderMap;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MapsSerializationTest {
    static {
        StoredArrayTest.serializer.register(ArMap.class, ArMap::serialize, ArMap::deserialize);
        StoredArrayTest.serializer.register(HMap.class, HMap::serialize, HMap::deserialize);
        StoredArrayTest.serializer.register(ArIndexedMap.class, ArIndexedMap::serialize, ArIndexedMap::deserialize);
        
        StoredArrayTest.serializer.register(ArOrderMap.class, ArOrderMap::serialize, ArOrderMap::deserialize);
        StoredArrayTest.serializer.register(HOrderMap.class, HOrderMap::serialize, HOrderMap::deserialize);
//        StoredArrayTest.serializer.register(ArOrderIndexedMap.class, ArOrderIndexedMap::serialize, ArOrderIndexedMap::deserialize);
    }

    private final static AddValue<StoredClass, String> changeValue = new SymmAddValue<StoredClass, String>() {
        public String addValue(StoredClass key, String prevValue, String newValue) {
            return newValue;
        }
    };
    
    @Test
    public void createAndSerializeArMap() {
        StoredArray<ArMap<StoredClass, String>> stored = new StoredArray<>(StoredArrayTest.serializer);
        ArMap<StoredClass, String> first = new ArMap<>(changeValue);
        ArMap<StoredClass, String> second = new ArMap<>(changeValue);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        String[] strArray = StoredTestDataGenerators.stringArray();
        fillMMap(array, strArray, first, second, stored);
        checkDeserializedEquality(array, strArray, stored);
    }
    
    @Test
    public void createAndSerializeHMap() {
        StoredArray<HMap<StoredClass, String>> stored = new StoredArray<>(StoredArrayTest.serializer);
        HMap<StoredClass, String> first = new HMap<>(changeValue);
        HMap<StoredClass, String> second = new HMap<>(changeValue);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        String[] strArray = StoredTestDataGenerators.stringArray();
        fillMMap(array, strArray, first, second, stored);
        checkDeserializedEquality(array, strArray, stored);
    }

    @Test
    public void createAndSerializeArIndexedMap() {
        StoredArray<ArIndexedMap<StoredClass, String>> stored = new StoredArray<>(StoredArrayTest.serializer);
        ArIndexedMap<StoredClass, String> first = new ArIndexedMap<>(changeValue);
        ArIndexedMap<StoredClass, String> second = new ArIndexedMap<>(changeValue);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        String[] strArray = StoredTestDataGenerators.stringArray();
        fillMMap(array, strArray, first, second, stored);
        checkDeserializedEquality(array, strArray, stored);
    }

    @Test
    public void createAndSerializeArOrderMap() {
        StoredArray<ArOrderMap<StoredClass, String>> stored = new StoredArray<>(StoredArrayTest.serializer);
        ArOrderMap<StoredClass, String> first = new ArOrderMap<>(changeValue);
        ArOrderMap<StoredClass, String> second = new ArOrderMap<>(changeValue);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        String[] strArray = StoredTestDataGenerators.stringArray();
        fillMOrderMap(array, strArray, first, second, stored);
        checkDeserializedOrderEquality(array, strArray, stored);
    }

    @Test
    public void createAndSerializeHOrderMap() {
        StoredArray<HOrderMap<StoredClass, String>> stored = new StoredArray<>(StoredArrayTest.serializer);
        HOrderMap<StoredClass, String> first = new HOrderMap<>(changeValue);
        HOrderMap<StoredClass, String> second = new HOrderMap<>(changeValue);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        String[] strArray = StoredTestDataGenerators.stringArray();
        fillMOrderMap(array, strArray, first, second, stored);
        checkDeserializedOrderEquality(array, strArray, stored);
    }

//    @Test
//    public void createAndSerializeArOrderIndexedMap() {
//        StoredArray<ArOrderIndexedMap<SerializableClass, String>> stored = new StoredArray<>(StoredArrayTest.serializer);
//        ArOrderIndexedMap<SerializableClass, String> first = new ArOrderIndexedMap<>(changeValue);
//        ArOrderIndexedMap<SerializableClass, String> second = new ArOrderIndexedMap<>(changeValue);
//        SerializableClass[] array = StoredArrayTest.initArray();
//        String[] strArray = StoredArrayTest.initStringArray();
//        fillMOrderMap(array, strArray, first, second, stored);
//        checkDeserializedOrderEquality(array, strArray, stored);
//    }
    
    private <T extends MMap<StoredClass, String>> void fillMMap(StoredClass[] array, String[] values, T first, T second, StoredArray<T> stored) {
        for (int i = 0; i < array.length / 2; ++i) {
            first.add(array[i], values[i]);
        }

        for (int i = array.length / 2; i < array.length; ++i) {
            second.add(array[i], values[i]);
        }

        stored.append(first);
        stored.append(second);
    }

    private <T extends MOrderMap<StoredClass, String>> void fillMOrderMap(StoredClass[] array, String[] values, T first, T second, StoredArray<T> stored) {
        for (int i = 0; i < array.length / 2; ++i) {
            first.add(array[i], values[i]);
        }

        for (int i = array.length / 2; i < array.length; ++i) {
            second.add(array[i], values[i]);
        }

        stored.append(first);
        stored.append(second);
    }
    
    private void checkDeserializedEquality(StoredClass[] array, String[] strArray, StoredArray<? extends ImMap<StoredClass, String>> stored) {
        ImMap<StoredClass, String> first = stored.get(0);
        ImMap<StoredClass, String> second = stored.get(1);
        
        int size1 = array.length / 2;
        int size2 = array.length - size1;
        
        assertEquals(first.size(), size1);
        assertEquals(second.size(), size2);

        for (int i = 0; i < size1; ++i) {
            assertEquals(strArray[i], first.get(array[i]));
        }
        for (int i = size1; i < array.length; ++i) {
            assertEquals(strArray[i], second.get(array[i]));
        }
    }

    private void checkDeserializedOrderEquality(StoredClass[] array, String[] strArray, StoredArray<? extends ImOrderMap<StoredClass, String>> stored) {
        ImOrderMap<StoredClass, String> first = stored.get(0);
        ImOrderMap<StoredClass, String> second = stored.get(1);

        int size1 = array.length / 2;
        int size2 = array.length - size1;

        assertEquals(first.size(), size1);
        assertEquals(second.size(), size2);

        for (int i = 0; i < size1; ++i) {
            assertEquals(strArray[i], first.get(array[i]));
        }
        for (int i = size1; i < array.length; ++i) {
            assertEquals(strArray[i], second.get(array[i]));
        }
    }
    
}
