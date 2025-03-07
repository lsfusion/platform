package lsfusion.base.col.implementations.abs;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.simple.FilterRevValueMap;
import lsfusion.base.col.implementations.simple.FilterValueMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.NotFunctionSet;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public abstract class ASet<K> extends ACol<K> implements ImSet<K> {

    public boolean intersect(ImSet<? extends K> ks) {
        if(size()>ks.size()) return ((ImSet<K>)ks).intersect(this);

        for(int i=0,size=size();i<size;i++)
            if(((ImSet<K>)ks).contains(get(i)))
                return true;
        return false;
    }

    public boolean intersectFn(FunctionSet<K> set) {
        if(set instanceof ImSet)
            return intersect((ImSet<? extends K>)set);

        if(set.isEmpty() || isEmpty())
            return false;

        if(set.isFull())
            return true;

        for(int i=0,size=size();i<size;i++)
            if(set.contains(get(i)))
                return true;
        return false;
    }

    public boolean disjoint(ImSet<? extends K> col) {
        if(size()>col.size()) return ((ImSet<K>)col).disjoint(this);

        for(int i=0,size=size();i<size;i++)
            if(((ImSet<K>)col).contains(get(i)))
                return false;
        return true;
    }

    public boolean containsAll(ImSet<? extends K> set) {
        if(set.size() > size())
            return false;

        for(int i=0,size=set.size();i<size;i++)
            if(!contains(set.get(i)))
                return false;
        return true;
    }

    public <G> ImMap<G, ImSet<K>> group(BaseUtils.Group<G, K> getter) {
        MExclMap<G, MExclSet<K>> mResult = MapFact.mExclMapMax(size());
        for (int i=0,size=size();i<size;i++) {
            K key = get(i);
            G group = getter.group(key);
            if(group!=null) {
                MExclSet<K> groupList = mResult.get(group);
                if (groupList == null) {
                    groupList = SetFact.mExclSetMax(size);
                    mResult.exclAdd(group, groupList);
                }
                groupList.exclAdd(key);
            }
        }
        return MapFact.immutable(mResult);
    }

    public ImSet<K> remove(final ImSet<? extends K> remove) {  // как правило внутренние, поэтому на disjoint нет смысла проверять
        return filterFn(new NotFunctionSet<>((FunctionSet<K>) remove));
    }

    @Override
    public ImSet<K> removeFn(FunctionSet<K> filter) {
        return filterFn(new NotFunctionSet<>(filter));
    }

    public ImSet<K> removeIncl(final ImSet<? extends K> remove) {  // как правило внутренние, поэтому на disjoint нет смысла проверять
        return remove(remove);
    }

    public ImSet<K> removeIncl(final K remove) { // assert что содержит
        assert contains(remove);

        return filterFn(element -> !BaseUtils.hashEquals(element, remove));
    }


    public boolean isFull() {
        return false;
    }

    public <V> ImCol<V> map(ImMap<K, ? extends V> map) {
        return ((ImMap<K, V>)map).filterIncl(this).values();
    }

    public <EV> ImSet<EV> mapRev(ImRevMap<K, EV> map) {
        return map.filterInclRev(this).valuesSet();
    }

    public ImSet<K> merge(ImSet<? extends K> merge) {
        if(merge.isEmpty()) return this; // полиморфизм по правому параметру
            
        if(size()<merge.size()) return ((ImSet<K>)merge).merge(this);

        MSet<K> mSet = SetFact.mSet(this);
        mSet.addAll(merge);
        return mSet.immutable();
    }

    public ImSet<K> merge(K element) {
        MSet<K> mSet = SetFact.mSet(this);
        mSet.add(element);
        return mSet.immutable();
    }

    public ImSet<K> addExcl(ImSet<? extends K> merge) {
        if(merge.isEmpty()) return this; // полиморфизм по правому параметру

        if(size()<merge.size()) return ((ImSet<K>)merge).addExcl(this);

        MExclSet<K> mSet = SetFact.mExclSet(this);
        mSet.exclAddAll(merge);
        return mSet.immutable();
    }

    public ImSet<K> addExcl(K element) {
        MExclSet<K> mSet = SetFact.mExclSet(this);
        mSet.exclAdd(element);
        return mSet.immutable();
    }

    public ImSet<K> filterFn(FunctionSet<K> filter) {
        if(filter.isEmpty()) return this;

        MFilterSet<K> mResult = SetFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            K element = get(i);
            if(filter.contains(element))
                mResult.keep(element);
        }
        return SetFact.imFilter(mResult, this);
    }

    public boolean containsFn(FunctionSet<K> filter) {
        if(filter.isEmpty()) return false;

        for(int i=0,size=size();i<size;i++) {
            K element = get(i);
            if(filter.contains(element))
                return true;
        }
        return false;
    }

    @Override
    public <E1 extends Exception, E2 extends Exception> ImSet<K> filterFnEx(ThrowingPredicate<K, E1, E2> filter) throws E1, E2 {
        MFilterSet<K> mResult = SetFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            K element = get(i);
            if(filter.test(element))
                mResult.keep(element);
        }
        return SetFact.imFilter(mResult, this);
    }

    public ImSet<K> split(FunctionSet<K> filter, Result<ImSet<K>> rest) {
        MFilterSet<K> mResult = SetFact.mFilter(this);
        MFilterSet<K> mRest = SetFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            K element = get(i);
            if(filter.contains(element))
                mResult.keep(element);
            else
                mRest.keep(element);
        }
        rest.set(SetFact.imFilter(mRest, this));
        return SetFact.imFilter(mResult, this);
    }

    public ImSet<K> split(ImSet<K> filter, Result<ImSet<K>> rest, Result<ImSet<K>> restSplit) {
        ImSet<K> common = split(filter, rest);
        restSplit.set(filter.remove(common));
        return common;
    }

    public ImSet<K> filter(ImSet<? extends K> filter) {
        if(size()>filter.size()) return ((ImSet<K>)filter).filter(this);
        return filterFn((FunctionSet<K>) filter);
    }

    public <V> ImMap<K, V> toMap(final V value) {
        return mapValues(() -> value);
    }

    public ImMap<K, K> toMap() {
        return mapValues((K value) -> value);
    }

    public ImRevMap<K, K> toRevMap() {
        return mapRevValues((K value) -> value);
    }

    public ImOrderSet<K> toOrderSet() {
        MOrderExclSet<K> mResult = SetFact.mOrderExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(get(i));
        return mResult.immutableOrder();
    }

    public ImOrderSet<K> sort() {
        List<K> sortList = new ArrayList<>(toJavaSet());
        Collections.sort(BaseUtils.<List<Comparable>>immutableCast(sortList));
        return SetFact.fromJavaOrderSet(sortList);
    }

    public ImOrderSet<K> sortSet(Comparator<K> comparator) {
        List<K> sortList = new ArrayList<>(toJavaSet());
        sortList.sort(comparator);
        return SetFact.fromJavaOrderSet(sortList);
    }

    public <M> ImSet<M> mapSetValues(Function<K, M> getter) {
        MExclSet<M> mResult = SetFact.mExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.apply(get(i)));
        return mResult.immutable();
    }

    public <M> ImMap<K, M> mapItValues(Function<K, M> getter) {
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(get(i)));
        return mvResult.immutableValue();
    }

    public <M> ImSet<M> mapItSetValues(Function<K, M> getter) {
        MExclSet<M> mResult = SetFact.mExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.apply(get(i)));
        return mResult.immutable();
    }

    public <M> ImMap<K, M> mapValues(Supplier<M> getter) {
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.get());
        return mvResult.immutableValue();
    }

    public <M> ImMap<K, M> mapValues(IntFunction<M> getter) {
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(i));
        return mvResult.immutableValue();
    }

    @Override
    public <M, E1 extends Exception, E2 extends Exception> ImMap<K, M> mapValuesEx(ThrowingFunction<K, M, E1, E2> getter) throws E1, E2 {
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(get(i)));
        return mvResult.immutableValue();
    }

    public <M> ImMap<K, M> mapValues(Function<K, M> getter) {
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(get(i)));
        return mvResult.immutableValue();
    }

    public <MK, MV> ImMap<MK, MV> mapKeyValues(Function<K, MK> getterKey, Function<K, MV> getterValue) {
        MExclMap<MK, MV> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++) {
            K element = get(i);
            mResult.exclAdd(getterKey.apply(element), getterValue.apply(element));
        }
        return mResult.immutable();
    }

    public <MK, MV> ImRevMap<MK, MV> mapRevKeyValues(Function<K, MK> getterKey, Function<K, MV> getterValue) {
        MRevMap<MK, MV> mResult = MapFact.mRevMap(size());
        for(int i=0,size=size();i<size;i++) {
            K element = get(i);
            mResult.revAdd(getterKey.apply(element), getterValue.apply(element));
        }
        return mResult.immutableRev();
    }

    public <M> ImRevMap<K, M> mapRevValues(IntFunction<M> getter) {
        ImRevValueMap<K, M> mResult = mapItRevValues();
        for(int i=0,size=size();i<size;i++)
            mResult.mapValue(i, getter.apply(i));
        return mResult.immutableValueRev();
    }

    public <M> ImRevMap<K, M> mapRevValues(IntObjectFunction<K, M> getter) {
        ImRevValueMap<K, M> mResult = mapItRevValues();
        for(int i=0,size=size();i<size;i++)
            mResult.mapValue(i, getter.apply(i, get(i)));
        return mResult.immutableValueRev();
    }

    public <M> ImRevMap<K, M> mapRevValues(Supplier<M> getter) {
        ImRevValueMap<K, M> mResult = mapItRevValues();
        for(int i=0,size=size();i<size;i++)
            mResult.mapValue(i, getter.get());
        return mResult.immutableValueRev();
    }

    public <M> ImRevMap<K, M> mapRevValues(Function<K, M> getter) {
        ImRevValueMap<K, M> mResult = mapItRevValues();
        for(int i=0,size=size();i<size;i++)
            mResult.mapValue(i, getter.apply(get(i)));
        return mResult.immutableValueRev();
    }

    public <M> ImRevMap<M, K> mapRevKeys(Supplier<M> getter) {
        MRevMap<M, K> mResult = MapFact.mRevMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.revAdd(getter.get(), get(i));
        return mResult.immutableRev();
    }

    public <M> ImRevMap<M, K> mapRevKeys(Function<K, M> getter) {
        MRevMap<M, K> mResult = MapFact.mRevMap(size());
        for(int i=0,size=size();i<size;i++) {
            K element = get(i);
            mResult.revAdd(getter.apply(element), element);
        }
        return mResult.immutableRev();
    }

    public <M> ImRevMap<M, K> mapRevKeys(IntFunction<M> getter) {
        MRevMap<M, K> mResult = MapFact.mRevMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.revAdd(getter.apply(i), get(i));
        return mResult.immutableRev();
    }

    public Set<K> toJavaSet() {
        Set<K> result = new HashSet<>();
        for(int i=0,size=size();i<size;i++)
            result.add(get(i));
        return result;
    }

    @Override
    public K getIdentIncl(K element) {
        for(int i=0,size=size();i<size;i++) {
            K obj = get(i);
            if(BaseUtils.hashEquals(obj, element))
                return obj;
        }
        assert false;
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if(this==obj) return true;
        if(!(obj instanceof ImSet))
            return obj instanceof ImCol && obj.equals(this);

        ImSet<K> set = (ImSet<K>)obj;
        if(set.size()!=size()) return false;

        for(int i=0,size=size();i<size;i++)
            if(!set.contains(get(i))) return false;
        return true;
    }

    public <M> ImFilterValueMap<K, M> mapFilterValues() {
        return new FilterValueMap<>(this.mapItValues());
    }
    public <M> ImFilterRevValueMap<K, M> mapFilterRevValues() {
        return new FilterRevValueMap<>(this.mapItRevValues());
    }

    @Override
    public ImSet<K> toSet() {
        return this;
    }

    @Override
    public ImList<K> toList() {
        return toOrderSet();
    }

    private final static AddValue<Object, ImSet<Object>> addMergeSet = new SymmAddValue<Object, ImSet<Object>>() {
        @Override
        public ImSet<Object> addValue(Object key, ImSet<Object> prevValue, ImSet<Object> newValue) {
            return prevValue.merge(newValue);
        }
    };

    public static <K, V> AddValue<K, ImSet<V>> addMergeSet() {
        return BaseUtils.immutableCast(addMergeSet);
    }
}
