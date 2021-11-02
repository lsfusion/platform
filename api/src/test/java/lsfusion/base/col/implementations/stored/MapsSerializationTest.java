package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.ArIndexedMap;
import lsfusion.base.col.implementations.ArMap;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.implementations.order.ArOrderMap;
import lsfusion.base.col.implementations.order.HOrderMap;
import lsfusion.base.col.implementations.stored.StoredArrayTest.SerializableClass;
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

    private final static AddValue<SerializableClass, String> changeValue = new SymmAddValue<SerializableClass, String>() {
        public String addValue(SerializableClass key, String prevValue, String newValue) {
            return newValue;
        }
    };
    
    @Test
    public void createAndSerializeArMap() {
        StoredArray<ArMap<SerializableClass, String>> stored = new StoredArray<>(StoredArrayTest.serializer);
        ArMap<SerializableClass, String> first = new ArMap<>(changeValue);
        ArMap<SerializableClass, String> second = new ArMap<>(changeValue);
        SerializableClass[] array = StoredArrayTest.initArray();
        String[] strArray = StoredArrayTest.initStringArray();
        fillMMap(array, strArray, first, second, stored);
        checkDeserializedEquality(array, strArray, stored);
    }
    
    @Test
    public void createAndSerializeHMap() {
        StoredArray<HMap<SerializableClass, String>> stored = new StoredArray<>(StoredArrayTest.serializer);
        HMap<SerializableClass, String> first = new HMap<>(changeValue);
        HMap<SerializableClass, String> second = new HMap<>(changeValue);
        SerializableClass[] array = StoredArrayTest.initArray();
        String[] strArray = StoredArrayTest.initStringArray();
        fillMMap(array, strArray, first, second, stored);
        checkDeserializedEquality(array, strArray, stored);
    }

    @Test
    public void createAndSerializeArIndexedMap() {
        StoredArray<ArIndexedMap<SerializableClass, String>> stored = new StoredArray<>(StoredArrayTest.serializer);
        ArIndexedMap<SerializableClass, String> first = new ArIndexedMap<>(changeValue);
        ArIndexedMap<SerializableClass, String> second = new ArIndexedMap<>(changeValue);
        SerializableClass[] array = StoredArrayTest.initArray();
        String[] strArray = StoredArrayTest.initStringArray();
        fillMMap(array, strArray, first, second, stored);
        checkDeserializedEquality(array, strArray, stored);
    }

    @Test
    public void createAndSerializeArOrderMap() {
        StoredArray<ArOrderMap<SerializableClass, String>> stored = new StoredArray<>(StoredArrayTest.serializer);
        ArOrderMap<SerializableClass, String> first = new ArOrderMap<>(changeValue);
        ArOrderMap<SerializableClass, String> second = new ArOrderMap<>(changeValue);
        SerializableClass[] array = StoredArrayTest.initArray();
        String[] strArray = StoredArrayTest.initStringArray();
        fillMOrderMap(array, strArray, first, second, stored);
        checkDeserializedOrderEquality(array, strArray, stored);
    }

    @Test
    public void createAndSerializeHOrderMap() {
        StoredArray<HOrderMap<SerializableClass, String>> stored = new StoredArray<>(StoredArrayTest.serializer);
        HOrderMap<SerializableClass, String> first = new HOrderMap<>(changeValue);
        HOrderMap<SerializableClass, String> second = new HOrderMap<>(changeValue);
        SerializableClass[] array = StoredArrayTest.initArray();
        String[] strArray = StoredArrayTest.initStringArray();
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
    
    private <T extends MMap<SerializableClass, String>> void fillMMap(SerializableClass[] array, String[] values, T first, T second, StoredArray<T> stored) {
        for (int i = 0; i < array.length / 2; ++i) {
            first.add(array[i], values[i]);
        }

        for (int i = array.length / 2; i < array.length; ++i) {
            second.add(array[i], values[i]);
        }

        stored.append(first);
        stored.append(second);
    }

    private <T extends MOrderMap<SerializableClass, String>> void fillMOrderMap(SerializableClass[] array, String[] values, T first, T second, StoredArray<T> stored) {
        for (int i = 0; i < array.length / 2; ++i) {
            first.add(array[i], values[i]);
        }

        for (int i = array.length / 2; i < array.length; ++i) {
            second.add(array[i], values[i]);
        }

        stored.append(first);
        stored.append(second);
    }
    
    private void checkDeserializedEquality(SerializableClass[] array, String[] strArray, StoredArray<? extends ImMap<SerializableClass, String>> stored) {
        ImMap<SerializableClass, String> first = stored.get(0);
        ImMap<SerializableClass, String> second = stored.get(1);
        
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

    private void checkDeserializedOrderEquality(SerializableClass[] array, String[] strArray, StoredArray<? extends ImOrderMap<SerializableClass, String>> stored) {
        ImOrderMap<SerializableClass, String> first = stored.get(0);
        ImOrderMap<SerializableClass, String> second = stored.get(1);

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
