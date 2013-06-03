package lsfusion.base.col;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.implementations.*;
import lsfusion.base.col.implementations.order.ArOrderSet;
import lsfusion.base.col.implementations.order.HOrderSet;
import lsfusion.base.col.implementations.simple.EmptyOrderSet;
import lsfusion.base.col.implementations.simple.EmptySet;
import lsfusion.base.col.implementations.simple.SingletonOrderSet;
import lsfusion.base.col.implementations.simple.SingletonSet;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SetFact {

    // IMMUTABLE

    public static <T> ImSet<T> EMPTY() {
        return EmptySet.INSTANCE();
    }

    public static <T> ImOrderSet<T> EMPTYORDER() {
        return EmptyOrderSet.INSTANCE();
    }

    public static <T> ImSet<T> singleton(T element) {
        return new SingletonSet<T>(element);
    }

    public static <T> ImOrderSet<T> singletonOrder(T element) {
        return new SingletonOrderSet<T>(element);
    }

    public static <T> ImSet<T> toSet(T... elements) {
        MSet<T> mSet = SetFact.mSetMax(elements.length);
        for(int i=0;i<elements.length;i++)
            mSet.add(elements[i]);
        return mSet.immutable();
    }

    public static <T> ImSet<T> toExclSet(int size, T[] elements) { // сзади null'ы могут быть
        MExclSet<T> mSet = SetFact.mExclSet(size);
        for(int i=0;i<size;i++)
            mSet.exclAdd(elements[i]);
        return mSet.immutable();
    }

    public static <T> ImSet<T> toExclSet(T... elements) {
        return toExclSet(elements.length, elements);
    }

    public static <K> ImOrderSet<K> toOrderExclSet(final K... array) {
        return toOrderExclSet(array.length, new GetIndex<K>() {
            public K getMapValue(int i) {
                return array[i];
            }});
    }

    public static <K> ImOrderSet<K> toOrderExclSet(int size, GetIndex<K> getter) {
        MOrderExclSet<K> mSet = SetFact.mOrderExclSet(size);
        for(int i=0;i<size;i++)
            mSet.exclAdd(getter.getMapValue(i));
        return mSet.immutableOrder();
    }

    public static <T> ImSet<T> add(ImSet<T>... sets) {
        MSet<T> mSet = SetFact.mSet(sets[0]);
        for(int i=1;i<sets.length;i++)
            mSet.addAll(sets[i]);
        return mSet.immutable();
    }

    private static <T> ImSet<T> calcAnd(ImSet<T>... sets) {
        ImSet<T> result = sets[0];
        for(int i=1;i<sets.length;i++)
            result = result.filter(sets[i]);
        return result;
    }

    public static <T> ImSet<T> and(ImSet<T>... sets) {
        ImSet<T> minSet = sets[0];
        for(int i=1;i<sets.length;i++)
            if(sets[i].size()<minSet.size())
                minSet = sets[i];

        MSet<T> mSet = SetFact.mSetMax(minSet.size());

        for(int i=0;i<minSet.size();i++) {
            T element = minSet.get(i);
            boolean all = true;
            for(ImSet<T> set : sets)
                if(set!=minSet && !set.contains(element)) {
                    all = false;
                    break;
                }
            if(all)
                mSet.add(element);
        }
        ImSet<T> set = mSet.immutable();
//        assert BaseUtils.hashEquals(set, calcAnd(sets));
        return set;
    }

    public static <K, KE extends K> boolean contains(K element, ImSet<KE> set) {
        return ((ImSet<K>)set).contains(element);
    }
    
    public static <T> ImSet<T> merge(ImSet<? extends T> set, T element) {
        return ((ImSet<T>)set).merge(element);
    }

    public static <T> ImSet<T> merge(ImSet<? extends T> set, ImSet<? extends T> merge) {
        return ((ImSet<T>)set).merge(merge);
    }

    public static <T> ImSet<T> addExcl(ImSet<? extends T> set, T element) {
        return ((ImSet<T>)set).addExcl(element);
    }

    public static <T> ImSet<T> addExcl(ImSet<? extends T> set1, ImSet<? extends T> set2) {
        return ((ImSet<T>)set1).addExcl(set2);
    }

    public static <T> ImSet<T> filter(ImSet<? extends T> set, ImSet<? extends T> filter) {
        return ((ImSet<T>)set).filter(filter);
    }

    public static <T> ImSet<T> remove(ImSet<? extends T> set, ImSet<? extends T> remove) {
        return ((ImSet<T>)set).remove(remove);
    }

    public static <T> ImOrderSet<T> addOrderExcl(ImOrderSet<? extends T> set1, ImOrderSet<? extends T> set2) {
        return ((ImOrderSet<T>)set1).addOrderExcl(set2);
    }
    
    // MUTABLE

    public static <K> MSet<K> mSet() {
        return new HSet<K>();
    }

    public static <K> MSet<K> mSetMax(int size) {
        if(size < useArrayMax)
            return new ArSet<K>(size);
        return new HSet<K>(size);
    }

    public static <K> MSet<K> mSet(ImSet<? extends K> set) {
        if(set instanceof HSet)
            return new HSet<K>((HSet<K>)set);
        else {
            MSet<K> mSet = mSet();
            mSet.addAll(set);
            return mSet;
        }
    }

    public final static int useArrayMax = 4;
    public final static int useIndexedArrayMin = 4;
    public final static float factorNotResize = 1.0f;
    public final static int useIndexedAddInsteadOfMerge = 1;

    // exclusive

    public static <K> MExclSet<K> mExclSet() {
        return new HSet<K>();
    }

    public static <K> MExclSet<K> mExclSet(int size) {
        return mExclSetMax(size);
    }

    public static <K> MExclSet<K> mExclSetMax(int size) {
        if(size<useArrayMax || size >= useIndexedArrayMin) // если слишком мало или много элементов используем массивы
            return new ArSet<K>(size);
        return new HSet<K>(size);
    }

    public static <K> MExclSet<K> mExclSet(ImSet<? extends K> set) {
        if(set instanceof HSet)
            return new HSet<K>((HSet<K>)set);

        MExclSet<K> mSet = mExclSet();
        mSet.exclAddAll(set);
        return mSet;
    }
    
    public static <K> MFilterSet<K> mFilter(ImSet<K> set) {  // assert что в том же порядке
        int size = set.size();
        if(set instanceof ArIndexedSet)
            return new ArIndexedSet<K>(size);
        if(size < useArrayMax)
            return new ArSet<K>(size);
        return new HSet<K>(size);
    }
    public static <K> ImSet<K> imFilter(MFilterSet<K> mResult, ImSet<K> set) {
        ImSet<K> result = mResult.immutable();
        if(result.size()==set.size()) {
            assert BaseUtils.hashEquals(result, set);
            return set; // чтобы сохранить ссылку
        }
        return result;
    }

    public static <K, V> MFilterSet<K> mFilter(ImMap<K, V> set) { // assert что в правильном порядке
        int size = set.size();
        if(set instanceof ArIndexedMap)
            return new ArIndexedSet<K>(size);
        if(size < useArrayMax)
            return new ArSet<K>(size);
        return new HSet<K>(size);
    }
    public static <K, V> ImSet<K> imFilter(MFilterSet<K> mResult, ImMap<K, V> set) {
        ImSet<K> result = mResult.immutable();
        if(result.size()==set.size()) {
            assert BaseUtils.hashEquals(result, set.keys());
            return set.keys(); // чтобы сохранить ссылку
        }
        return result;
    }

    // order

    public static <K> MOrderSet<K> mOrderSet() {
        return new HOrderSet<K>();
    }

    public static <K> MOrderSet<K> mOrderSet(ImOrderSet<? extends K> set) {
        if(set instanceof HOrderSet)
            return new HOrderSet<K>((HOrderSet<K>)set);

        MOrderSet<K> mSet = SetFact.mOrderSet();
        mSet.addAll(set);
        return mSet;
    }

    public static <K> MOrderSet<K> mOrderSet(int size) {
        return mOrderSetMax(size);
    }

    public static <K> MOrderSet<K> mOrderSetMax(int size) {
        if(size<useArrayMax)
            return new ArOrderSet<K>(size);
        return new HOrderSet<K>(size);
    }

    public static <K> MOrderExclSet<K> mOrderExclSet() {
        return new HOrderSet<K>();
    }

    public static <K> MOrderExclSet<K> mOrderExclSet(ImOrderSet<? extends K> set) {
        if(set instanceof HOrderSet)
            return new HOrderSet<K>((HOrderSet<K>)set);

        MOrderExclSet<K> mSet = SetFact.mOrderExclSet();
        mSet.exclAddAll(set);
        return mSet;
    }

    public static <K> MOrderExclSet<K> mOrderExclSet(int size) {
        return mOrderExclSetMax(size);
    }

    public static <K> MOrderExclSet<K> mOrderExclSetMax(int size) {
        if(size<useArrayMax || size >= useIndexedArrayMin) // если слишком мало или много элементов используем массивы
            return new ArOrderSet<K>(size);
        return new HOrderSet<K>(size);
    }

    public static <K> MOrderFilterSet<K> mOrderFilter(ImOrderSet<K> filter) { // assert что в том же порядке
        int size = filter.size();
//        if(filter instanceof ArOrderIndexedSet) keep сложнее поддерживать
//            return new ArOrderIndexedSet<K>(size);
        if(size < useArrayMax || size >= useIndexedArrayMin)
            return new ArOrderSet<K>(size);
        return new HOrderSet<K>(size);
    }
    public static <K> ImOrderSet<K> imOrderFilter(MOrderFilterSet<K> mResult, ImOrderSet<K> filter) {
        ImOrderSet<K> result = mResult.immutableOrder();
        if(result.size()==filter.size()) {
            assert BaseUtils.hashEquals(result, filter);
            return filter; // чтобы сохранить ссылку
        }
        return result;
    }
    public static <K, V> MOrderFilterSet<K> mOrderFilter(ImOrderMap<K, V> filter) { // assert что в том же порядке
        int size = filter.size();
//        if(filter instanceof ArOrderIndexedMap) keep сложнее поддерживать
//            return new ArOrderIndexedSet<K>(size);
        if(size < useArrayMax ||  size >= useIndexedArrayMin)
            return new ArOrderSet<K>(size);
        return new HOrderSet<K>(size);
    }
    public static <K, V> ImOrderSet<K> imOrderFilter(MOrderFilterSet<K> mResult, ImOrderMap<K, V> filter) {
        ImOrderSet<K> result = mResult.immutableOrder();
        if(result.size()==filter.size()) {
            assert BaseUtils.hashEquals(result, filter.keyOrderSet());
            return filter.keyOrderSet(); // чтобы сохранить ссылку
        }
        return result;
    }


    // map'ы по определению mutable, без явных imutable интерфейсов

    public static <K> MAddSet<K> mAddSet() {
        return new HSet<K>();
    }

    public static <K> MAddSet<K> mAddSet(ImSet<? extends K> set) {
        if(set instanceof HSet)
            return new HSet<K>((HSet<K>)set);

        MAddSet<K> mResult = mAddSet();
        mResult.addAll(set);
        return mResult;
    }

    public static <K> Set<K> mAddRemoveSet() {
        return new HashSet<K>();
    }

    public static <T> Set<T> mAddRemoveSet(ImSet<? extends T> set) {
        Set<T> result = new HashSet<T>();
        for(int i=0,size=set.size();i<size;i++)
            result.add(set.get(i));
        return result;
    }

    // remove при необходимости получения mutable интерфейсов

    public static <T> ImSet<T> fromJavaSet(Set<T> set) {
        if(set.isEmpty()) // оптимизация
            return SetFact.EMPTY();

        MExclSet<T> mSet = SetFact.mExclSet(set.size());
        for(T element : set)
            mSet.exclAdd(element);
        return mSet.immutable();
    }

    public static <T> ImOrderSet<T> fromJavaOrderSet(List<T> set) {
        MOrderExclSet<T> mSet = SetFact.mOrderExclSet(set.size());
        for(T element : set)
            mSet.exclAdd(element);
        return mSet.immutableOrder();
    }

    public static <T> void addJavaAll(Set<T> set, ImSet<T> add) {
        for(T element : add)
            set.add(element);
    }
}
