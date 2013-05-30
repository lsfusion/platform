package platform.server.data.expr.where;

import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

public interface CaseExprInterface {

    public abstract void add(Where where, Expr data);
    public abstract Expr getFinal();

}
