package lsfusion.base.col.implementations.order;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.ArIndexedSet;
import lsfusion.base.col.implementations.ArSet;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.implementations.HSet;
import lsfusion.base.col.implementations.abs.AMWrapOrderSet;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class HOrderSet<K> extends AMWrapOrderSet<K, HSet<K>> {

    // mutable конструктор
    public HOrderSet() {
        super(new HSet<>());
    }

    public HOrderSet(HSet<K> wrapSet) {
        super(wrapSet);
    }

    public HOrderSet(int size) {
        super(new HSet<>(size));
    }

    public HOrderSet(HOrderSet<K> orderSet) {
        super(new HSet<>(orderSet.wrapSet));
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new HOrderMap<>(this);
    }

    public <M> ImRevValueMap<K, M> mapItOrderRevValues() { // предполагается заполнение в том же порядке
        return new HMap<>(wrapSet);
    }

    public ImOrderSet<K> immutableOrder() {
        if(wrapSet.size()==0)
            return SetFact.EMPTYORDER();
        if(wrapSet.size()==1)
            return SetFact.singletonOrder(single());

        if (!wrapSet.isStored()) {
            if (wrapSet.size() < SetFact.useArrayMax) {
                Object[] array = new Object[wrapSet.size()];
                for (int i = 0; i < wrapSet.size(); i++)
                    array[i] = get(i);
                return new ArOrderSet<>(new ArSet<>(wrapSet.size(), array));
            }
            if (wrapSet.size() >= SetFact.useIndexedArrayMin) {
                Object[] array = new Object[wrapSet.size()];
                for (int i = 0; i < wrapSet.size(); i++)
                    array[i] = get(i);
                int[] order = new int[wrapSet.size()];
                ArSet.sortArray(wrapSet.size(), array, order);
                return new ArOrderIndexedSet<>(new ArIndexedSet<>(wrapSet.size(), array), order);
            }

            if (wrapSet.getIndexes().length > wrapSet.size() * SetFact.factorNotResize) {
                int[] newIndexes = new int[wrapSet.size()];
                System.arraycopy(wrapSet.getIndexes(), 0, newIndexes, 0, wrapSet.size());
                wrapSet.setIndexes(newIndexes);
            }
        }
        return this;
    }

    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        HOrderSet<?> set = (HOrderSet<?>) o;
        serializer.serialize(set.wrapSet, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        HSet<?> set = (HSet<?>) serializer.deserialize(inStream);
        return new HOrderSet<>(set);
    }
}
