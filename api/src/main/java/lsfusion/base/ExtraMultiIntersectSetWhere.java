package lsfusion.base;

public abstract class ExtraMultiIntersectSetWhere<T, This extends ExtraMultiIntersectSetWhere<T,This>> extends ExtraSetWhere<T,This> {

    public ExtraMultiIntersectSetWhere() {
    }

    public ExtraMultiIntersectSetWhere(T[] wheres) {
        super(wheres);
    }

    public ExtraMultiIntersectSetWhere(T where) {
        super(where);
    }

    protected abstract This FALSETHIS();
    protected abstract T[] intersect(T where1, T where2);

    public This intersect(This where) {
        if(isTrue() || where.isFalse()) return where;
        if(isFalse() || where.isTrue()) return (This) this;

        This result = FALSETHIS();
        for(T andOp : where.wheres)
            for(T and : wheres)
                result = result.add(createThis(intersect(andOp,and)));
        return result;
    }

}
