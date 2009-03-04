package platform.server.data.query.wheres;

import platform.server.where.Where;

import java.util.HashMap;

public class MapWhere<T> extends HashMap<T,Where> {

    public void add(T object, Where where) {
        Where inWhere = get(object);
        if(inWhere!=null)
            inWhere = inWhere.or(where);
        else
            inWhere = where;
        put(object,inWhere);
    }
}
