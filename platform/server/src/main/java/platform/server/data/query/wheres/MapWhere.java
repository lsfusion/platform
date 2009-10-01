package platform.server.data.query.wheres;

import platform.base.QuickMap;
import platform.server.where.Where;

public class MapWhere<T> extends QuickMap<T,Where> {

    protected Where addValue(Where prevValue, Where newValue) {
        return prevValue.or(newValue);
    }

    protected boolean containsAll(Where who, Where what) {
        throw new RuntimeException("not supported");
    }

/*    public void add(T object, Where where) {
        Where inWhere = get(object);
        if(inWhere!=null)
            inWhere = inWhere.or(where);
        else
            inWhere = where;
        put(object,inWhere);
    }*/
}
