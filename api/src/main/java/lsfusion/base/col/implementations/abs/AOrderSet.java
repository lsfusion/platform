package lsfusion.base.col.implementations.abs;

import lsfusion.base.BaseUtils;
import lsfusion.base.FunctionSet;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.NotFunctionSet;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;

public abstract class AOrderSet<K> extends AList<K> implements ImOrderSet<K> {

    public boolean contains(K element) {
        return getSet().contains(element);
    }

    public <V> ImOrderMap<K, V> toOrderMap(final V value) {
        return mapOrderValues(new GetStaticValue<V>() {
            public V getMapValue() {
                return value;
            }});
    }

    public ImOrderSet<K> addOrderExcl(ImOrderSet<? extends K> map) {
        if(map.isEmpty()) return this; // полиморфизм по правой части

        MOrderExclSet<K> mResult = SetFact.mOrderExclSet(this);
        mResult.exclAddAll(map);
        return mResult.immutableOrder();
    }

    public ImOrderSet<K> addOrderExcl(K element) {
        MOrderExclSet<K> mResult = SetFact.mOrderExclSet(this);
        mResult.exclAdd(element);
        return mResult.immutableOrder();
    }

    public ImOrderSet<K> mergeOrder(ImOrderSet<? extends K> col) {
        MOrderSet<K> mResult = SetFact.mOrderSet(this);
        mResult.addAll(col);
        return mResult.immutableOrder();
    }

    public ImOrderSet<K> mergeOrder(K element) {
        MOrderSet<K> mResult = SetFact.mOrderSet(this);
        mResult.add(element);
        return mResult.immutableOrder();
    }

    public <V> ImRevMap<K, V> mapSet(final ImOrderSet<? extends V> set) {
        return mapOrderRevValues(new GetIndex<V>() {
            public V getMapValue(int i) {
                return set.get(i);
            }});
    }

    public <V> ImMap<K, V> mapList(final ImList<? extends V> list) {
        return mapOrderValues(new GetIndex<V>() {
            public V getMapValue(int i) {
                return list.get(i);
            }});
    }

    public ImOrderSet<K> filterOrder(FunctionSet<K> filter) {
        MOrderFilterSet<K> mResult = SetFact.mOrderFilter(this);
        for(int i=0,size=size();i<size;i++) {
            K element = get(i);
            if(filter.contains(element))
                mResult.keep(element);
        }
        return SetFact.imOrderFilter(mResult, this);
    }

    public ImOrderSet<K> removeOrder(ImSet<? extends K> ks) {
        return filterOrder(new NotFunctionSet<K>((FunctionSet<K>) ks));
    }

    public ImOrderSet<K> removeOrderIncl(K remove) {
        assert contains(remove);

        MOrderExclSet<K> mResult = SetFact.mOrderExclSet(size() - 1);
        for(int i=0,size=size();i<size;i++) {
            K element = get(i);
            if(!BaseUtils.hashEquals(remove, element))
                mResult.exclAdd(element);
        }
        return mResult.immutableOrder();
    }

    @Override
    public ImList<K> reverseList() {
        return reverseOrder();
    }

    public ImOrderSet<K> reverseOrder() {
        MOrderExclSet<K> mResult = SetFact.mOrderExclSet(size());
        for(int i=size()-1;i>=0;i--)
            mResult.exclAdd(get(i));
        return mResult.immutableOrder();
    }

    public <V> ImOrderSet<V> mapOrder(ImRevMap<? extends K, ? extends V> imRevMap) {
        return mapOrder((ImMap<? extends K, ? extends V>) imRevMap);
    }

    public <V> ImOrderSet<V> mapOrder(ImMap<? extends K, ? extends V> imMap) {
        return mapOrderSetValues(((ImRevMap<K, V>)imMap).fnGetValue());
    }

    public <V> ImOrderMap<K, V> mapOrderMap(ImMap<K, V> map) {
        return mapOrderValues(map.fnGetValue());
    }

    public ImOrderSet<K> filterOrderIncl(ImSet<? extends K> ks) {
        assert getSet().containsAll(ks);
        return filterOrder((FunctionSet<K>) ks);
    }

    public ImOrderSet<K> subOrder(int from, int to) {
        MOrderExclSet<K> mResult = SetFact.mOrderExclSet(to-from);
        for(int j=from;j<to;j++)
            mResult.exclAdd(get(j));
        return mResult.immutableOrder();
    }

    public <M> ImOrderSet<M> mapOrderSetValues(GetValue<M, K> getter) {
        MOrderExclSet<M> mResult = SetFact.mOrderExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.getMapValue(get(i)));
        return mResult.immutableOrder();
    }

    public <M> ImOrderSet<M> mapMergeOrderSetValues(GetValue<M, K> getter) {
        MOrderSet<M> mResult = SetFact.mOrderSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.getMapValue(get(i)));
        return mResult.immutableOrder();
    }

    public <M> ImOrderSet<M> mapOrderSetValues(GetIndexValue<M, K> getter) {
        MOrderExclSet<M> mResult = SetFact.mOrderExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.getMapValue(i, get(i)));
        return mResult.immutableOrder();
    }

    public <M> ImOrderMap<K, M> mapOrderValues(GetValue<M, K> getter) {
        ImOrderValueMap<K, M> mvResult = mapItOrderValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(get(i)));
        return mvResult.immutableValueOrder();
    }

    public <M> ImOrderMap<K, M> mapOrderValues(GetStaticValue<M> getter) {
        ImOrderValueMap<K, M> mvResult = mapItOrderValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue());
        return mvResult.immutableValueOrder();
    }

    public <M> ImMap<K, M> mapOrderValues(GetIndexValue<M, K> getter) {
        ImOrderValueMap<K, M> mvResult = mapItOrderValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(i, get(i)));
        return mvResult.immutableValueOrder().getMap();
    }

    public <M> ImMap<K, M> mapOrderValues(GetIndex<M> getter) {
        ImOrderValueMap<K, M> mvResult = mapItOrderValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(i));
        return mvResult.immutableValueOrder().getMap();
    }

    public <M> ImRevMap<K, M> mapOrderRevValues(GetIndex<M> getter) {
        ImRevValueMap<K, M> mvResult = mapItOrderRevValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(i));
        return mvResult.immutableValueRev();
    }

    public <M> ImRevMap<K, M> mapOrderRevValues(GetIndexValue<M, K> getter) {
        ImRevValueMap<K, M> mvResult = mapItOrderRevValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.getMapValue(i, get(i)));
        return mvResult.immutableValueRev();
    }

    public <M> ImRevMap<M, K> mapOrderRevKeys(GetIndex<M> getter) {
        MRevMap<M,K> mResult = MapFact.mRevMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.revAdd(getter.getMapValue(i), get(i));
        return mResult.immutableRev();
    }

    public <M> ImRevMap<M, K> mapOrderRevKeys(GetIndexValue<M, K> getter) {
        MRevMap<M,K> mResult = MapFact.mRevMap(size());
        for(int i=0,size=size();i<size;i++) {
            K element = get(i);
            mResult.revAdd(getter.getMapValue(i, element), element);
        }
        return mResult.immutableRev();
    }

    public ImCol<K> getCol() {
        return getSet();
    }
}
