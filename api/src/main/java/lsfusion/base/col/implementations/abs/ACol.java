package lsfusion.base.col.implementations.abs;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.IntObjectFunction;
import lsfusion.base.lambda.set.FunctionSet;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

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
        MMap<K, Integer> mResult = MapFact.mMapMax(size(), MapFact.addLinear());
        for(int i=0,size=size();i<size;i++)
            mResult.add(get(i), 1);
        return mResult.immutable();
    }

    public <M> ImCol<M> mapColValues(IntObjectFunction<K, M> getter) {
        MCol<M> mResult = ListFact.mCol(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.apply(i, get(i)));
        return mResult.immutableCol();
    }

    public <M> ImCol<M> mapColValues(Function<K, M> getter) {
        MCol<M> mResult = ListFact.mCol(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.apply(get(i)));
        return mResult.immutableCol();
    }

    public <M> ImMap<M, K> mapColKeys(IntFunction<M> getter) {
        MExclMap<M,K> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.apply(i), get(i));
        return mResult.immutable();
    }

    public <M> ImSet<M> mapColSetValues(Function<K, M> getter) {
        MExclSet<M> mResult = SetFact.mExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.apply(get(i)));
        return mResult.immutable();
    }

    public <M> ImSet<M> mapColSetValues(IntObjectFunction<K, M> getter) {
        MExclSet<M> mResult = SetFact.mExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.apply(i, get(i)));
        return mResult.immutable();
    }

    public <M> ImSet<M> mapMergeSetValues(Function<K, M> getter) {
        MSet<M> mResult = SetFact.mSetMax(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.apply(get(i)));
        return mResult.immutable();
    }

    public <M> ImSet<M> mapMergeSetSetValues(Function<K, ImSet<M>> getter) {
        MSet<M> mResult = SetFact.mSet();
        for(int i=0,size=size();i<size;i++)
            mResult.addAll(getter.apply(get(i)));
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

    public ImList<K> sort(Comparator<K> comparator) {
        List<K> sortList = new ArrayList<>(toList().toJavaList());
        sortList.sort(comparator);
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
    public int immutableHashCode() { // should match SingletonSet hashCode
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

    public boolean contains(K element) {
        for(int i=0,size=size();i<size;i++)
            if(BaseUtils.hashEquals(get(i), element))
                return true;
        return false;
    }
}
