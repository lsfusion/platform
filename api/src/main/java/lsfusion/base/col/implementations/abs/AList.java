package lsfusion.base.col.implementations.abs;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.IntObjectFunction;
import lsfusion.base.lambda.set.FunctionSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

// почти все его методы в ACol, для множественного наследования
public abstract class AList<K> extends AColObject implements ImList<K> {

    // дублирует ACol, так как там порядок может меняться
    public Iterator<K> iterator() {
        return new Iterator<K>() {
            int i=0;

            public boolean hasNext() {
                return i<size();
            }

            public K next() {
                return get(i++);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public String toString() {
        return toString(",");
    }

    public String toString(String separator) {
        StringBuilder builder = new StringBuilder();
        for(int i=0,size=size();i<size;i++) {
            if(i!=0)
                builder.append(separator);
            builder.append(get(i).toString());
        }
        return builder.toString();
    }

    public String toString(Function<K, String> getter, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for(int i=0,size=size();i<size;i++) {
            if(i!=0)
                builder.append(delimiter);
            builder.append(getter.apply(get(i)));
        }
        return builder.toString();
    }

    public String toString(Supplier<String> getter, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for(int i=0,size=size();i<size;i++) {
            if(i!=0)
                builder.append(delimiter);
            builder.append(getter.get());
        }
        return builder.toString();
    }

    public String toString(IntObjectFunction<K, String> getter, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for(int i=0,size=size();i<size;i++) {
            if(i!=0)
                builder.append(delimiter);
            builder.append(getter.apply(i, get(i)));
        }
        return builder.toString();
    }

    public boolean isEmpty() {
        return getCol().isEmpty();
    }

    public K single() {
        return getCol().single();
    }

    public int indexOf(K key) {
        for(int i=0,size=size();i<size;i++)
            if(BaseUtils.hashEquals(get(i), key))
                return i;
        return -1;
    }

    public boolean containsNull() {
        for(int i=0,size=size();i<size;i++)
            if(get(i) == null)
                return true;
        return false;
    }

    public ImMap<Integer, K> toIndexedMap() {
        return mapListMapValues(i -> i);
    }

    public ImList<K> addList(ImList<? extends K> list) {
        MList<K> mResult = ListFact.mList(this);
        mResult.addAll(list);
        return mResult.immutableList();
    }

    public ImList<K> addList(K element) {
        MList<K> mResult = ListFact.mList(this);
        mResult.add(element);
        return mResult.immutableList();
    }

    public ImList<K> subList(int from, int to) {
        MList<K> mResult = ListFact.mList(to-from);
        for(int j=from;j<to;j++)
            mResult.add(get(j));
        return mResult.immutableList();
    }

    public ImList<K> remove(int i) {
        MList<K> mResult = ListFact.mList(size() - 1);
        for(int j=0,size=size();j<size;j++)
            if(j != i)
                mResult.add(get(j));
        return mResult.immutableList();
    }

    public ImList<K> replace(int i, K element) {
        MList<K> mResult = ListFact.mList(size());
        for(int j=0,size=size();j<size;j++)
            mResult.add(j == i ? element : get(j));
        return mResult.immutableList();
    }

    public List<K> toJavaList() {
        List<K> result = new ArrayList<>();
        for(K element : this)
            result.add(element);
        return result;
    }

    public ImList<K> reverseList() {
        MList<K> mResult = ListFact.mList(size());
        for(int i=size()-1;i>=0;i--)
            mResult.add(get(i));
        return mResult.immutableList();
    }

    public ImList<K> filterList(FunctionSet<K> filter) {
        MList<K> mList = ListFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            K element = get(i);
            if(filter.contains(element))
                mList.add(element);
        }
        return ListFact.imFilter(mList, this);
    }

    public ImOrderSet<K> toOrderSet() {
        MOrderSet<K> mResult = SetFact.mOrderSetMax(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(get(i));
        return mResult.immutableOrder();
    }

    public ImOrderSet<K> toOrderExclSet() {
        MOrderExclSet<K> mResult = SetFact.mOrderExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(get(i));
        return mResult.immutableOrder();
    }

    public <M> ImList<M> mapItListValues(Function<K, M> getter) {
        MList<M> mResult = ListFact.mList(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.apply(get(i)));
        return mResult.immutableList();
    }

    public <M> ImList<M> mapListValues(IntObjectFunction<K, M> getter) {
        MList<M> mResult = ListFact.mList(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.apply(i, get(i)));
        return mResult.immutableList();
    }

    public <M> ImList<M> mapListValues(Function<K, M> getter) {
        MList<M> mResult = ListFact.mList(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.apply(get(i)));
        return mResult.immutableList();
    }

    public <M> ImMap<M, K> mapListMapValues(IntFunction<M> getterKey) {
        MExclMap<M, K> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getterKey.apply(i), get(i));
        return mResult.immutable();
    }

    public <MK, MV> ImMap<MK, MV> mapListKeyValues(IntFunction<MK> getterKey, Function<K, MV> getterValue) {
        MExclMap<MK, MV> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getterKey.apply(i), getterValue.apply(get(i)));
        return mResult.immutable();
    }

    public <MK, MV> ImMap<MK, MV> mapListKeyValues(Function<K, MK> getterKey, Function<K, MV> getterValue) {
        MExclMap<MK, MV> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++) {
            K key = get(i);
            mResult.exclAdd(getterKey.apply(key), getterValue.apply(key));
        }
        return mResult.immutable();
    }

    public <MK, MV> ImRevMap<MK, MV> mapListRevKeyValues(IntFunction<MK> getterKey, Function<K, MV> getterValue) {
        MRevMap<MK, MV> mResult = MapFact.mRevMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.revAdd(getterKey.apply(i), getterValue.apply(get(i)));
        return mResult.immutableRev();
    }

    public <V> ImList<V> mapList(ImMap<? extends K, ? extends V> imMap) {
        return mapListValues(((ImMap<K, V>)imMap).fnGetValue());
    }

    public K[] toArray(K[] array) {
        assert size()==array.length;
        for(int i=0;i<array.length;i++)
            array[i] = get(i);
        return array;
    }

    @Override
    public boolean equals(Object obj) {
        if(this==obj) return true;
        if(!(obj instanceof ImList)) return false;

        ImList<K> list = (ImList<K>)obj;
        if(list.size()!=size()) return false;

        for(int i=0,size=size();i<size;i++)
            if(!BaseUtils.hashEquals(get(i), list.get(i)))
                return false;
        return true;
    }

    public int immutableHashCode() {
        int hashCode = 1;
        for (int i=0,size=size();i<size;i++)
            hashCode = 31 * hashCode + get(i).hashCode();
        return hashCode;
    }

    public K last() {
        return get(size() - 1);
    }

    public <G> ImMap<G, ImList<K>> groupList(BaseUtils.Group<G, K> getter) {
        MExclMap<G, MList<K>> mResult = MapFact.mExclMapMax(size());
        for (int i=0,size=size();i<size;i++) {
            K key = get(i);
            G group = getter.group(key);
            if(group!=null) {
                MList<K> groupList = mResult.get(group);
                if (groupList == null) {
                    groupList = ListFact.mList();
                    mResult.exclAdd(group, groupList);
                }
                groupList.add(key);
            }
        }
        return MapFact.immutableList(mResult);
    }

}
