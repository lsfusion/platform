package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.ArIndexedMap;
import lsfusion.base.col.implementations.ArIndexedSet;
import lsfusion.base.col.implementations.stored.StoredArrayTest.SerializableClass;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArIndexedTest {
    static {
        StoredArraySerializerRegistry serializer = (StoredArraySerializerRegistry) StoredArraySerializer.getInstance();
        serializer.register(SerializableClass.class, SerializableClass::serialize, SerializableClass::deserialize);
    }

    private final int SIZE = 10000; 
    
    @Test
    public void checkArIndexedSetKeepTransformation() {
        SerializableClass[] arr = generateSerializableArray();
        ArIndexedSet<SerializableClass> set = new ArIndexedSet<>(SIZE);
        for (SerializableClass cls : arr) {
            set.keep(cls);
        }
        
        assertTrue(set.isStored());
        assertEquals(set.size(), SIZE); 
        for (int i = SIZE - 1; i >= 0; --i) {
            assertEquals(arr[i], set.get(i));
        }
    }

    @Test
    public void checkArIndexedSetImmutableTransformation() {
        SerializableClass[] arr = generateSerializableArray();
        ArIndexedSet<SerializableClass> set = new ArIndexedSet<>(arr.length, arr);
        ImSet<SerializableClass> storedSet = set.immutable();   

        assertTrue(storedSet instanceof StoredArIndexedSet);
        assertEquals(storedSet.size(), set.size());
        for (int i = set.size()-1; i >= 0; --i) {
            assertEquals(storedSet.get(i), set.get(i));
        }
    }
    
    @Test
    public void checkArIndexedSetConstructorTransformation() {
        SerializableClass[] arr = generateSerializableArray();
        ArIndexedSet<SerializableClass> set = new ArIndexedSet<>(arr.length, arr);
        ArIndexedSet<SerializableClass> targetSet = new ArIndexedSet<>(set);

        assertTrue(targetSet.isStored());
        assertEquals(targetSet.size(), set.size());
        for (int i = set.size() - 1; i >= 0; --i) {
            assertEquals(targetSet.get(i), set.get(i));
        }
    }
    
    @Test
    public void checkArIndexedMapImmutableTransformation() {
        SerializableClass[] keys = generateSerializableArray();
        String[] values = generateStringArray();
        ArIndexedMap<SerializableClass, String> map = new ArIndexedMap<>(SIZE, keys, values);
        
        ImMap<SerializableClass, String> storedMap = map.immutable();

        assertTrue(storedMap instanceof StoredArIndexedMap);
        assertEquals(storedMap.size(), map.size());
        for (int i = 0; i < map.size(); ++i) {
            assertEquals(storedMap.getKey(i), map.getKey(i));
            assertEquals(storedMap.getValue(i), map.getValue(i));
        }
        for (int i = map.size()-1; i >= 0; --i) {
            assertEquals(storedMap.get(keys[i]), map.get(keys[i]));
        }
    }

    @Test
    public void checkArIndexedMapConstructorTransformation() {
        SerializableClass[] keys = generateSerializableArray();
        String[] values = generateStringArray();
        ArIndexedMap<SerializableClass, String> map = new ArIndexedMap<>(keys.length, keys, values);
        ArIndexedMap<SerializableClass, String> storedMap = new ArIndexedMap<>(map, true);

        assertTrue(storedMap.isStored());
        assertEquals(storedMap.size(), keys.length);
        for (int i = 0; i < storedMap.size(); ++i) {
            assertEquals(storedMap.getKey(i), keys[i]);
            assertEquals(storedMap.getValue(i), values[i]);
        }
    }
    
    private SerializableClass[] generateSerializableArray() {
        List<SerializableClass> list = new ArrayList<>();
        for (int i = 0; i < SIZE; ++i) {
            list.add(new SerializableClass("Name", i+1, true));
        }
        list.sort(Comparator.comparingInt(SerializableClass::hashCode));
        return list.toArray(new SerializableClass[0]);
    }
    
    private String[] generateStringArray() {
        String[] res = new String[SIZE];
        Random rand = new Random();
        for (int i = 0; i < SIZE; ++i) {
            res[i] = "String" + rand.nextInt(SIZE);
        }
        return res;
    }
}
