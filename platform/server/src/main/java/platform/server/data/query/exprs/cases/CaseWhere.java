package platform.server.data.query.exprs.cases;

import platform.server.where.Where;

public interface CaseWhere<C> {
    Where getCaseWhere(C cCase);
}
