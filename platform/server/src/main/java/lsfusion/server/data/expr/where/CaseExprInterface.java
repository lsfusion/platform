package lsfusion.server.data.expr.where;

import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;

public interface CaseExprInterface {

    public abstract void add(Where where, Expr data);
    public abstract Expr getFinal();

}
