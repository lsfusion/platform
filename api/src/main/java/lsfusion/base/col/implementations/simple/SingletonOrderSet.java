package lsfusion.base.col.implementations.simple;

import lsfusion.base.col.implementations.abs.AOrderSet;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class SingletonOrderSet<K> extends AOrderSet<K> {

    private SingletonSet<K> set;

    public SingletonOrderSet(K key) {
        set = new SingletonSet<>(key);
    }

    public SingletonOrderSet(SingletonSet<K> set) {
        this.set = set;
    }

    public int size() {
        return 1;
    }

    public K get(int i) {
        assert i==0;
        return set.get(0);
    }

    public ImSet<K> getSet() {
        return set;
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new SingletonOrderMap<>(set.get(0));
    }

    public <M> ImRevValueMap<K, M> mapItOrderRevValues() {
        return new SingletonRevMap<>(set.get(0));
    }

    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        SingletonOrderSet<?> set = (SingletonOrderSet<?>) o;
        serializer.serialize(set.set, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        SingletonSet<?> set = (SingletonSet<?>) serializer.deserialize(inStream);
        return new SingletonOrderSet<>(set);
    }
}
