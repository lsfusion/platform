package platform.server.data.where;

import platform.base.QuickMap;

public abstract class DNFWheres<M extends DNFWheres.Interface<M>,C extends CheckWhere,This extends DNFWheres<M,C,This>> extends QuickMap<M,C> {

    public static interface Interface<I extends Interface> {
        I and(I and);

        boolean isFalse();
    }

    public void or(This joins) {
        addAll(joins);
    }

    protected abstract C andValue(C prevValue, C newValue);

    protected boolean containsAll(C who, C what) {
        return who.means(what);
    }

    protected abstract This createThis();

    public This and(This joins) {
        This result = createThis();
        // берем все пары joins'ов
        for(int i1=0;i1<size;i1++)
            for(int i2=0;i2<joins.size;i2++) {
                M andJoin = getKey(i1).and(joins.getKey(i2));
                C andWhere = andValue(getValue(i1), joins.getValue(i2));
                if(!andJoin.isFalse() && !andWhere.not().checkTrue()) // тут isFalse'а достаточно так как AB=>!(IaIb) <=> ABIaIb==FALSE, A=>Ia,B=>Ib <=> AB==FALSE ???
                    result.add(andJoin, andWhere);
            }
        return result;
    }

    protected DNFWheres() {
    }

    protected DNFWheres(M key, C value) {
        super(key, value);
    }
}
