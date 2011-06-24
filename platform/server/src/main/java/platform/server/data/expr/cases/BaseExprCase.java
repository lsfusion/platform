package platform.server.data.expr.cases;

import platform.server.data.expr.BaseExpr;
import platform.server.data.where.Where;

public class BaseExprCase extends Case<BaseExpr> {

    public BaseExprCase(Where where, BaseExpr data) {
        super(where, data);
    }

    public BaseExprCase(BaseExpr data) {
        this(Where.TRUE, data);
    }

    public BaseExprCase and(Where and) {
        return new BaseExprCase(where.and(and), data);
    }
}
