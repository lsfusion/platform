package platform.server.data.query.exprs.cases;

import platform.server.where.Where;

public abstract class CaseWhereInterface<C> {
    public abstract Where getWhere(C cCase);

    public Where getElse() {
        return Where.FALSE;
    }
}
