package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.stored.StoredArrayTest.SerializableClass;

import java.util.Arrays;
import java.util.Comparator;

public class StoredArIndexedMapTest {
    
    private SerializableClass[] initSortedArray() {
        SerializableClass[] array = StoredArrayTest.initArray();
        Arrays.sort(array, Comparator.comparingInt(SerializableClass::hashCode));
        return array;
    }

//    private StoredArIndexedSet<SerializableClass> initMap(SerializableClass[] arr) {
//        StoredArray<SerializableClass> stored = new StoredArray<>(arr, StoredArrayTest.serializer);
//        StoredArray<String>  
//        StoredArIndexedSet<SerializableClass> result = new StoredArIndexedSet<>(stored);
//        return result;
//    }
}