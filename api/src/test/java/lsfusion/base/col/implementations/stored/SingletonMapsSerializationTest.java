package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.simple.SingletonOrderMap;
import lsfusion.base.col.implementations.simple.SingletonOrderSet;
import lsfusion.base.col.implementations.simple.SingletonRevMap;
import lsfusion.base.col.implementations.simple.SingletonSet;
import lsfusion.base.col.implementations.stored.StoredTestDataGenerators.StoredClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SingletonMapsSerializationTest {
    static {
        StoredArrayTest.serializer.register(SingletonRevMap.class, SingletonRevMap::serialize, SingletonRevMap::deserialize);
        StoredArrayTest.serializer.register(SingletonOrderMap.class, SingletonOrderMap::serialize, SingletonOrderMap::deserialize);
        StoredArrayTest.serializer.register(SingletonSet.class, SingletonSet::serialize, SingletonSet::deserialize);
        StoredArrayTest.serializer.register(SingletonOrderSet.class, SingletonOrderSet::serialize, SingletonOrderSet::deserialize);
    }
    
    @Test
    public void createAndSerializeRevMap() {
        StoredArray<SingletonRevMap<StoredClass, String>> stored = new StoredArray<>(StoredArrayTest.serializer);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        String[] strings = StoredTestDataGenerators.stringArray();
        for (int i = 0; i < array.length; ++i) {
            stored.append(new SingletonRevMap<>(array[i], strings[i]));
        }
        
        for (int i = 0; i < array.length; ++i) {
            SingletonRevMap<StoredClass, String> map = stored.get(i);
            assertEquals(map.getKey(0), array[i]);
            assertEquals(map.getValue(0), strings[i]);
            assertEquals(map.get(map.getKey(0)), strings[i]);
        }
    }

    @Test
    public void createAndSerializeOrderMap() {
        StoredArray<SingletonOrderMap<StoredClass, String>> stored = new StoredArray<>(StoredArrayTest.serializer);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        String[] strings = StoredTestDataGenerators.stringArray();
        for (int i = 0; i < array.length; ++i) {
            stored.append(new SingletonOrderMap<>(array[i], strings[i]));
        }

        for (int i = 0; i < array.length; ++i) {
            SingletonOrderMap<StoredClass, String> map = stored.get(i);
            assertEquals(map.getKey(0), array[i]);
            assertEquals(map.getValue(0), strings[i]);
            assertEquals(map.get(map.getKey(0)), strings[i]);
        }
    }

    @Test
    public void createAndSerializeSet() {
        StoredArray<SingletonSet<StoredClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        for (int i = 0; i < array.length; ++i) {
            stored.append(new SingletonSet<>(array[i]));
        }

        for (int i = 0; i < array.length; ++i) {
            SingletonSet<StoredClass> set = stored.get(i);
            assertEquals(set.get(0), array[i]);
        }
    }

    @Test
    public void createAndSerializeOrderSet() {
        StoredArray<SingletonOrderSet<StoredClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        StoredClass[] array = StoredTestDataGenerators.simpleArray();
        for (int i = 0; i < array.length; ++i) {
            stored.append(new SingletonOrderSet<>(array[i]));
        }

        for (int i = 0; i < array.length; ++i) {
            SingletonOrderSet<StoredClass> set = stored.get(i);
            assertEquals(set.get(0), array[i]);
        }
    }
    
}
