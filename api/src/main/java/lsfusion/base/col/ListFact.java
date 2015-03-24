package lsfusion.base.col;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.implementations.ArCol;
import lsfusion.base.col.implementations.order.ArList;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddCol;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;

import java.util.*;

public class ListFact {

    // IMMUTABLE

    public static <T> ImList<T> EMPTY() {
        return SetFact.EMPTYORDER();
    }

    public static <T> ImList<T> singleton(T element) {
        return SetFact.singletonOrder(element);
    }

    public static ImOrderSet<Integer> consecutiveList(int size, final int is) {
        return SetFact.toOrderExclSet(size, new GetIndex<Integer>() {
            public Integer getMapValue(int i) {
                return i+is;
            }});
    }

    public static ImOrderSet<Integer> consecutiveList(int size) {
        return consecutiveList(size, 1);
    }

    public static <T> ImList<T> toList(final T... array) {
        return toList(array.length, new GetIndex<T>() {
            public T getMapValue(int i) {
                return array[i];
            }
        });
    }

    public static <T> ImList<T> toList(int size, GetIndex<T> getter) {
        MList<T> mList = ListFact.mList(size);
        for(int i=0;i<size;i++)
            mList.add(getter.getMapValue(i));
        return mList.immutableList();
    }

    public static <T> ImList<T> toList(final T value, int size) {
        return toList(size, new GetIndex<T>() {
            public T getMapValue(int i) {
                return value;
            }});
    }

    public static <T> ImList<T> add(T element, ImList<? extends T> list) {
        MList<T> mList = ListFact.mList(list.size()+1);
        mList.add(element);
        mList.addAll(list);
        return mList.immutableList();
    }

    public static <T> ImList<T> add(ImList<? extends T> list1, ImList<? extends T> list2) {
        return ((ImList<T>)list1).addList(list2);
    }

    public static <T> ImCol<T> fromJavaCol(Collection<T> col) {
        MCol<T> mCol = ListFact.mCol(col.size());
        for(T element : col)
            mCol.add(element);
        return mCol.immutableCol();
    }

    public static <T> ImList<T> fromJavaList(List<T> col) {
        MList<T> mList = ListFact.mList(col.size());
        for(T element : col)
            mList.add(element);
        return mList.immutableList();
    }

    public static <T> ImList<T> fromIndexedMap(final ImMap<Integer, T> map) {
        return toList(map.size(), new GetIndex<T>() {
            public T getMapValue(int i) {
                return map.get(i);
            }});
    }

    public static <T> ImOrderSet<T> fromIndexedMap(final ImRevMap<Integer, T> map) {
        return SetFact.toOrderExclSet(map.size(), new GetIndex<T>() {
            public T getMapValue(int i) {
                return map.get(i);
            }});
    }
    
    public static <K, V> List<Map<K, V>> toJavaMapList(ImOrderSet<ImMap<K, V>> listMap) {
        List<Map<K, V>> list = new ArrayList<Map<K, V>>();
        for(ImMap<K, V> map : listMap)
            list.add(map.toJavaMap());
        return list;
    }


    // MUTABLE

    public static <K> MCol<K> mCol() {
        return new ArCol<K>();
    }

    public static <K> MCol<K> mCol(int size) {
        return new ArCol<K>(size);
    }

    public static <K> MCol<K> mCol(ImCol<? extends K> col) {
        if(col instanceof ArCol)
            return new ArCol<K>((ArCol<K>) col);

        MCol<K> mCol = mCol();
        mCol.addAll(col);
        return mCol;
    }

    public static <K> MCol<K> mColMax(int size) {
        return new ArCol<K>(size);
    }

    public static <K> MCol<K> mColFilter(ImCol<? extends K> col) {
        return mColMax(col.size());
    }

    public static <K> ImCol<K> imColFilter(MCol<K> mCol, ImCol<? extends K> col) {
        ImCol<K> result = mCol.immutableCol();
        if(result.size()==col.size()) {
            assert BaseUtils.hashEquals(result, col);
            return (ImCol<K>) col;
        }
        return result;
    }

    public static <K> MList<K> mList() {
        return new ArList<K>();
    }

    public static <K> MList<K> mList(int size) {
        return new ArList<K>(size);
    }

    public static <K> MList<K> mListMax(int size) {
        return new ArList<K>(size);
    }

    public static <K> MList<K> mList(ImList<? extends K> list) {
        if(list instanceof ArList)
            return new ArList<K>((ArList<K>) list);

        MList<K> mList = mList(list.size());
        mList.addAll(list);
        return mList;
    }

    public static <K> MList<K> mFilter(ImList<K> list) {
        return mListMax(list.size());
    }

    public static <K> ImList<K> imFilter(MList<K> mList, ImList<K> list) {
        ImList<K> result = mList.immutableList();
        if(result.size()==list.size()) {
            assert BaseUtils.hashEquals(result, list);
            return list;
        }
        return result;
    }

    // map'ы по определению mutable, без явных imutable интерфейсов

    public static <K> MAddCol<K> mAddCol() {
        return new ArCol<K>();
    }

    public static <K> Collection<K> mAddRemoveCol() {
        return new ArrayList<K>();
    }

    public static <K> List<K> mAddRemoveList() {
        return new ArrayList<K>();
    }
    
    public static <K> void addJavaAll(ImCol<K> list, Collection<K> mList) {
        for(int i=0,size=list.size();i<size;i++)
            mList.add(list.get(i));
    }

    private final static GetIndex<MSet<Object>> mSet = new GetIndex<MSet<Object>>() {
        public MSet<Object> getMapValue(int i) {
            return SetFact.mSet();
        }
    };

    public static <V> GetIndex<MSet<V>> mSet() {
        return BaseUtils.immutableCast(mSet);
    }

}
