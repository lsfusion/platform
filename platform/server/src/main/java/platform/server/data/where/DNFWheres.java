package platform.server.data.where;

import platform.base.QuickMap;

public abstract class DNFWheres<M extends DNFWheres.Interface<M>,This extends DNFWheres<M,This>> extends QuickMap<M,Where> {

    public static interface Interface<I extends Interface> {
        I and(I and);

        boolean isFalse();
    }

    // Where не будет использоваться
    protected abstract boolean privateWhere();

    public void or(This joins) {
        addAll(joins);
    }

    protected Where addValue(Where prevValue, Where newValue) {
        if(privateWhere())
            return prevValue.orMeans(newValue);
        else
            return prevValue.or(newValue);
    }

    protected boolean containsAll(Where who, Where what) {
        return what.means(who);
    }

    protected abstract This createThis();

    public This and(This joins) {
        This result = createThis();
        // берем все пары joins'ов
        for(int i1=0;i1<size;i1++)
            for(int i2=0;i2<joins.size;i2++) {
                M andJoin = getKey(i1).and(joins.getKey(i2));
                Where andWhere;
                if(privateWhere())
                    andWhere = getValue(i1).andMeans(joins.getValue(i2));
                else
                    andWhere = getValue(i1).and(joins.getValue(i2));
                if(!andJoin.isFalse() && !andWhere.isFalse()) // тут isFalse'а достаточно так как AB=>!(IaIb) <=> ABIaIb==FALSE, A=>Ia,B=>Ib <=> AB==FALSE ???
                    result.add(andJoin, andWhere);
            }
        return result;
    }

    protected DNFWheres() {
    }

    protected DNFWheres(M key, Where value) {
        super(key, value);
    }
}
