package lsfusion.server.data.expr.where;

import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;

public interface CaseExprInterface {

    void add(Where where, Expr data);
    Expr getFinal();

}
