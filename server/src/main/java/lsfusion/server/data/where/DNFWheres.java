package lsfusion.server.data.where;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.WrapMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MMap;

public abstract class DNFWheres<M extends DNFWheres.Interface<M>,C,This extends DNFWheres<M,C,This>> extends WrapMap<M,C> {

    public static interface Interface<I extends Interface> {
        I and(I and);
    }

    protected abstract C andValue(M key, C prevValue, C newValue);

    protected abstract AddValue<M, C> getAddValue();
    protected abstract This createThis(ImMap<M, C> map);
    protected abstract boolean valueIsFalse(C value);

    public This and(This joins) {
        int size1=size(); int size2=joins.size();
        MMap<M, C> result = MapFact.mMapMax(size1 * size2, getAddValue());
        // берем все пары joins'ов
        for(int i1=0;i1<size1;i1++)
            for(int i2=0;i2<size2;i2++) {
                M andJoin = getKey(i1).and(joins.getKey(i2));
                C andWhere = andValue(andJoin, getValue(i1), joins.getValue(i2));
                if(!valueIsFalse(andWhere)) // тут isFalse'а достаточно так как AB=>!(IaIb) <=> ABIaIb==FALSE, A=>Ia,B=>Ib <=> AB==FALSE ???
                    result.add(andJoin, andWhere);
            }
        return createThis(result.immutable());
    }

    protected DNFWheres(M key, C value) {
        super(key, value);
    }

    protected DNFWheres(ImMap<M, C> map) {
        super(map);
    }
}
