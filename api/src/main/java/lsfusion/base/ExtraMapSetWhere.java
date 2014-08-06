package lsfusion.base;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;

public abstract class ExtraMapSetWhere<K,V,T extends ImMap<K,V>, This extends ExtraMapSetWhere<K,V,T,This>> extends ExtraIntersectSetWhere<T,This> {

    protected ExtraMapSetWhere() {
    }

    protected ExtraMapSetWhere(T[] wheres) {
        super(wheres);
    }

    protected ExtraMapSetWhere(T where) {
        super(where);
    }

    protected abstract T createMap(ImMap<K, V> map);
    protected abstract V addMapValue(V value1, V value2);

    protected T add(T addWhere, T[] wheres, int numWheres, T[] proceeded, int numProceeded) {
        T added;
        for(int j=0;j<numWheres;j++) // проверим, если add'ся, вырезаем вкинем в конец
            if(wheres[j]!=null && (added = add(wheres[j],addWhere))!=null) {
                wheres[j]=null;
                return added;
            }
        return null;
    }

    protected T add(T where1, T where2) {
        if(where1.size()==where2.size()) {
            ImFilterValueMap<K, V> mvResult = where1.mapFilterValues();
            K keyAdd = null; V value1Add = null; V value2Add = null;
            for(int i=0, size = where1.size();i<size;i++) {
                K key1 = where1.getKey(i);
                V value2 = where2.getPartial(key1);
                if(value2!=null) {
                    V value1 = where1.getValue(i);
                    if(value2.equals(value1))
                        mvResult.mapValue(i, value2);
                    else
                        if(keyAdd!=null)
                            return null;
                        else {
                            keyAdd = key1;
                            value1Add = value1;
                            value2Add = value2;
                        }
                } else
                    return null;
            }
            ImMap<K, V> result = mvResult.immutableValue();

            assert keyAdd!=null; // полностью одинаковые элементы должный уйти containsAll'ами
            V addValue = addMapValue(value1Add,value2Add);
            if(addValue==null)
                return null;
            else
                result = result.addExcl(keyAdd,addValue);
            return createMap(result);
        }
        return null;
    }
}
