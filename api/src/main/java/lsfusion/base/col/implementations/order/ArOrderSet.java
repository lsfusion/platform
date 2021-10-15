package lsfusion.base.col.implementations.order;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.ArIndexedSet;
import lsfusion.base.col.implementations.ArMap;
import lsfusion.base.col.implementations.ArSet;
import lsfusion.base.col.implementations.abs.AMWrapOrderSet;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ArOrderSet<K> extends AMWrapOrderSet<K, ArSet<K>> {

    public ArOrderSet() {
        super(new ArSet<>());
    }

    public ArOrderSet(int size) {
        super(new ArSet<>(size));
    }

    public ArOrderSet(ArSet<K> wrapSet) {
        super(wrapSet);
    }

    public ArOrderSet(ArOrderSet<K> orderSet) {
        super(new ArSet<>(orderSet.wrapSet));
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new ArOrderMap<>(this);
    }

    public <M> ImRevValueMap<K, M> mapItOrderRevValues() { // предполагается заполнение в том же порядке
        return new ArMap<>(wrapSet);
    }

    public ImOrderSet<K> immutableOrder() {
        if(wrapSet.size==0)
            return SetFact.EMPTYORDER();
        if(wrapSet.size==1)
            return SetFact.singletonOrder(single());

        if(wrapSet.array.length > wrapSet.size * SetFact.factorNotResize) {
            Object[] newArray = new Object[wrapSet.size];
            System.arraycopy(wrapSet.array, 0, newArray, 0, wrapSet.size);
            wrapSet.array = newArray;
        }

        if(wrapSet.size < SetFact.useArrayMax)
            return this;

        // упорядочиваем Set
        int[] order = new int[wrapSet.size];
        ArSet.sortArray(wrapSet.size, wrapSet.array, order);
        return new ArOrderIndexedSet<>(new ArIndexedSet<>(wrapSet.size, wrapSet.array), order);
    }

    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        ArOrderSet<?> set = (ArOrderSet<?>) o;
        serializer.serialize(set.wrapSet, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        ArSet<?> set = (ArSet<?>) serializer.deserialize(inStream);
        return new ArOrderSet<>(set);
    }
}
