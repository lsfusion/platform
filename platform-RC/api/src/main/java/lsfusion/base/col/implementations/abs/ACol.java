package lsfusion.base.col.implementations.abs;

import lsfusion.base.FunctionSet;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;

import java.util.*;

public abstract class ACol<K> extends AColObject implements ImCol<K> {

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

    public boolean isEmpty() {
        return size() == 0;
    }

    public K single() {
        assert size() == 1;
        return get(0);
    }

    public ImSet<K> toSet() {
        MSet<K> mResult = SetFact.mSetMax(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(get(i));
        return mResult.immutable();
    }

    public ImCol<K> mergeCol(ImCol<K> ks) {
        if(ks.size() > size())
            return ks.mergeCol(this);

        MCol<K> mCol = ListFact.mCol(this);
        mCol.addAll(ks);
        return mCol.immutableCol();
    }

    public ImCol<K> addCol(K element) {
        MCol<K> mCol = ListFact.mCol(this);
        mCol.add(element);
        return mCol.immutableCol();
    }

    public ImCol<K> filterCol(FunctionSet<K> filter) {
        MCol<K> mCol = ListFact.mColFilter(this);
        for(int i=0,size=size();i<size;i++) {
            K element = get(i);
            if(filter.contains(element))
                mCol.add(element);
        }
        return ListFact.imColFilter(mCol, this);
    }

    public ImMap<K, Integer> multiSet() {
        MMap<K, Integer> mResult = MapFact.mMapMax(size(), MapFact.<K>addLinear());
        for(int i=0,size=size();i<size;i++)
            mResult.add(get(i), 1);
        return mResult.immutable();
    }

    public <M> ImCol<M> mapColValues(GetIndexValue<M, K> getter) {
        MCol<M> mResult = ListFact.mCol(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.getMapValue(i, get(i)));
        return mResult.immutableCol();
    }

    public <M> ImCol<M> mapColValues(GetValue<M, K> getter) {
        MCol<M> mResult = ListFact.mCol(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.getMapValue(get(i)));
        return mResult.immutableCol();
    }

    public <M> ImMap<M, K> mapColKeys(GetIndex<M> getter) {
        MExclMap<M,K> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.getMapValue(i), get(i));
        return mResult.immutable();
    }

    public <M> ImSet<M> mapColSetValues(GetValue<M, K> getter) {
        MExclSet<M> mResult = SetFact.mExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.getMapValue(get(i)));
        return mResult.immutable();
    }

    public <M> ImSet<M> mapColSetValues(GetIndexValue<M, K> getter) {
        MExclSet<M> mResult = SetFact.mExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.getMapValue(i, get(i)));
        return mResult.immutable();
    }

    public <M> ImSet<M> mapMergeSetValues(GetValue<M, K> getter) {
        MSet<M> mResult = SetFact.mSetMax(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.getMapValue(get(i)));
        return mResult.immutable();
    }

    public ImList<K> toList() {
        MList<K> mResult = ListFact.mList(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(get(i));
        return mResult.immutableList();
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

    public ImList<K> sort(Comparator<K> comparator) {
        List<K> sortList = new ArrayList<>(toList().toJavaList());
        Collections.sort(sortList, comparator);
        return ListFact.fromJavaList(sortList);
    }

    public Collection<K> toJavaCol() {
        List<K> result = new ArrayList<>();
        for(K element : this)
            result.add(element);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this==obj) return true;
        if(!(obj instanceof ImCol)) return false;

        ImCol<K> set = (ImCol<K>)obj;
        return multiSet().equals(set.multiSet());
    }

    @Override
    public int immutableHashCode() {
        int hash = 0;
        for(int i=0;i<size();i++)
            hash = hash + get(i).hashCode();
        return hash * 31;
    }


    public K[] toArray(K[] array) {
        assert size() == array.length;
        for(int i=0,size=size();i<size;i++)
            array[i] = get(i);
        return array;
    }
}
