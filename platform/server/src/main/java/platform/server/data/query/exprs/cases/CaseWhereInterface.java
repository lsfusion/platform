package platform.server.data.query.exprs.cases;

import platform.server.where.Where;

public interface CaseWhereInterface<C> {
    Where getWhere(C cCase);
}
