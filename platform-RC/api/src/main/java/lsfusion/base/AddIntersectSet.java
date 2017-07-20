package lsfusion.base;

public abstract class AddIntersectSet<T,This extends AddIntersectSet<T,This>> extends AddSet<T, This> {

    protected AddIntersectSet() {
    }

    protected AddIntersectSet(T[] wheres) {
        super(wheres);
    }

    protected AddIntersectSet(T where) {
        super(where);
    }

    protected abstract T[] intersect(T element1, T element2);
    
    public This intersect(This where) {
        if(isTrue() || where.isFalse()) return where;
        if(isFalse() || where.isTrue()) return (This) this;

        This result = createThis(newArray(0));
        for(T andOp : where.wheres)
            for(T and : wheres)
                result = result.add(createThis(intersect(andOp, and)));
        return result;
    }

}
