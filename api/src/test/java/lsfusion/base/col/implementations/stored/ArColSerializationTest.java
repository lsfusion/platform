package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.ArCol;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArColSerializationTest {
    static {
        StoredArrayTest.serializer.register(ArCol.class, ArCol::serialize, ArCol::deserialize);
    }
    
    @Test
    public void createAndSerializeRevMap() {
        StoredArray<ArCol<StoredArrayTest.SerializableClass>> stored = new StoredArray<>(StoredArrayTest.serializer);
        StoredArrayTest.SerializableClass[] array = StoredArrayTest.initArrayWithNulls();
        ArCol<StoredArrayTest.SerializableClass> first = new ArCol<>();
        for (int i = 0; i < array.length / 2; ++i) {
            first.add(array[i]);
        }

        ArCol<StoredArrayTest.SerializableClass> second = new ArCol<>();
        for (int i = array.length / 2; i < array.length; ++i) {
            second.add(array[i]);
        }
        
        int oldsizeFirst = first.size;
        int oldsizeSecond = second.size;
        
        stored.append(first);
        stored.append(second);
        
        first = stored.get(0);
        second = stored.get(1);
        
        assertEquals(first.size, oldsizeFirst);
        assertEquals(second.size, oldsizeSecond);
        
        for (int i = 0; i < array.length / 2; ++i) {
            assertEquals(array[i], first.get(i));
        }
        for (int i = array.length / 2; i < array.length; ++i) {
            assertEquals(array[i], second.get(i - array.length / 2));
        }
    }
    
}
