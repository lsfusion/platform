package platform.server.data.query.exprs.cases;

import platform.server.where.Where;

public abstract class Case<D> {
    public Where where;
    public D data;

    Case(Where iWhere,D iData) {
        where = iWhere;
        data = iData;
    }
}
