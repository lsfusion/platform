package lsfusion.base.col.implementations.abs;

import lsfusion.base.BaseUtils;
import lsfusion.base.FunctionSet;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public String toString(GetValue<String, K> getter, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for(int i=0,size=size();i<size;i++) {
            if(i!=0)
                builder.append(delimiter);
            builder.append(getter.getMapValue(get(i)));
        }
        return builder.toString();
    }

    public String toString(GetStaticValue<String> getter, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for(int i=0,size=size();i<size;i++) {
            if(i!=0)
                builder.append(delimiter);
            builder.append(getter.getMapValue());
        }
        return builder.toString();
    }

    public String toString(GetIndexValue<String, K> getter, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for(int i=0,size=size();i<size;i++) {
            if(i!=0)
                builder.append(delimiter);
            builder.append(getter.getMapValue(i, get(i)));
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

    public ImMap<Integer, K> toIndexedMap() {
        return mapListMapValues(new GetIndex<Integer>() {
            public Integer getMapValue(int i) {
                return i;
            }
        });
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

    public List<K> toJavaList() {
        List<K> result = new ArrayList<K>();
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

    public <M> ImList<M> mapItListValues(GetValue<M, K> getter) {
        MList<M> mResult = ListFact.mList(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.getMapValue(get(i)));
        return mResult.immutableList();
    }

    public <M> ImList<M> mapListValues(GetIndex<M> getter) {
        MList<M> mResult = ListFact.mList(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.getMapValue(i));
        return mResult.immutableList();
    }

    public <M> ImList<M> mapListValues(GetIndexValue<M, K> getter) {
        MList<M> mResult = ListFact.mList(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.getMapValue(i, get(i)));
        return mResult.immutableList();
    }

    public <M> ImList<M> mapListValues(GetValue<M, K> getter) {
        MList<M> mResult = ListFact.mList(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.getMapValue(get(i)));
        return mResult.immutableList();
    }

    public <M> ImMap<M, K> mapListMapValues(GetIndex<M> getterKey) {
        MExclMap<M, K> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getterKey.getMapValue(i), get(i));
        return mResult.immutable();
    }

    public <MK, MV> ImMap<MK, MV> mapListKeyValues(GetIndex<MK> getterKey, GetValue<MV, K> getterValue) {
        MExclMap<MK, MV> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getterKey.getMapValue(i), getterValue.getMapValue(get(i)));
        return mResult.immutable();
    }

    public <MK, MV> ImRevMap<MK, MV> mapListRevKeyValues(GetIndex<MK> getterKey, GetValue<MV, K> getterValue) {
        MRevMap<MK, MV> mResult = MapFact.mRevMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.revAdd(getterKey.getMapValue(i), getterValue.getMapValue(get(i)));
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
}
