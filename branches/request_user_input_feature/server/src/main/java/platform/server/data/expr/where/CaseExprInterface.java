package platform.server.data.expr.where;

import platform.server.data.where.Where;
import platform.server.data.expr.Expr;

public interface CaseExprInterface {

    public Where getUpWhere();
    public abstract void add(Where where, Expr data);
    public abstract Expr getFinal();

}
