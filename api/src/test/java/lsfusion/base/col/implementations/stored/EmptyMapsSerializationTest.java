package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.simple.EmptyOrderMap;
import lsfusion.base.col.implementations.simple.EmptyOrderSet;
import lsfusion.base.col.implementations.simple.EmptyRevMap;
import lsfusion.base.col.implementations.simple.EmptySet;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class EmptyMapsSerializationTest {
    static {
        StoredArrayTest.serializer.register(EmptySet.class, (o, s, outS) -> {}, (inS, s) -> EmptySet.INSTANCE());
        StoredArrayTest.serializer.register(EmptyOrderSet.class, (o, s, outS) -> {}, (inS, s) -> EmptyOrderSet.INSTANCE());
        StoredArrayTest.serializer.register(EmptyOrderMap.class, (o, s, outS) -> {}, (inS, s) -> EmptyOrderMap.INSTANCE());
        StoredArrayTest.serializer.register(EmptyRevMap.class, (o, s, outS) -> {}, (inS, s) -> EmptyRevMap.INSTANCE());
    }

    @Test
    public void createAndSerialize() {
        check(new StoredArray<>(StoredArrayTest.serializer), EmptySet.INSTANCE());
        check(new StoredArray<>(StoredArrayTest.serializer), EmptyOrderSet.INSTANCE());
        check(new StoredArray<>(StoredArrayTest.serializer), EmptyOrderMap.INSTANCE());
        check(new StoredArray<>(StoredArrayTest.serializer), EmptyRevMap.INSTANCE());
    }   
    
    private <T> void check(StoredArray<T> stored, T instance) {
        stored.append(instance);
        stored.append(null);
        stored.append(instance);

        assertSame(instance, stored.get(0));
        assertNull(stored.get(1));
        assertSame(instance, stored.get(2));
    }    
}
