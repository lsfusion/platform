package platform.base;

public abstract class SetWhere<T,This extends SetWhere<T,This>> extends AddSet<T,This> {

    protected SetWhere() {
        super();
    }

    protected SetWhere(T[] iWheres) {
        super(iWheres);
    }

    protected SetWhere(T where) {
        super(where);
    }

    protected abstract T[] intersect(T where1,T where2);

    protected This intersect(This where) {
        if(isTrue() || where.isFalse()) return where;
        if(isFalse() || where.isTrue()) return (This) this;

        This result = createThis(newArray(0));
        for(T andOp : where.wheres)
            for(T and : wheres)
                result = result.add(createThis(intersect(andOp,and)));
        return result;
    }
}
