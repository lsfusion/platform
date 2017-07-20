package lsfusion.base;

public abstract class ExtraSingleIntersectSetWhere<T, This extends ExtraSingleIntersectSetWhere<T,This>> extends ExtraSetWhere<T,This> {

    protected ExtraSingleIntersectSetWhere() {
    }

    protected ExtraSingleIntersectSetWhere(T[] wheres) {
        super(wheres);
    }

    protected ExtraSingleIntersectSetWhere(T where) {
        super(where);
    }

    // если не null то слились
    protected abstract T intersect(T where1, T where2);

    public This intersect(This intersect) {
        if(isTrue() || intersect.isFalse()) return intersect;
        if(isFalse() || intersect.isTrue()) return (This) this;

        T[] intersectWheres = newArray(wheres.length*intersect.wheres.length); int numWheres = 0;
        T resWhere;
        for(T where : wheres)
            for(T intWhere : intersect.wheres)
                if((resWhere=intersect(where,intWhere))!=null)
                    intersectWheres[numWheres++] = resWhere;
        return createThis(add(newArray(wheres.length*intersect.wheres.length), 0, intersectWheres, numWheres, false));
    }


}
