package lsfusion.server.logics.property;

import lsfusion.server.classes.ObjectValueClassSet;
import lsfusion.server.data.Table;
import lsfusion.server.data.expr.Expr;

public interface ClassField {

    Table.Join.Expr getInconsistentExpr(Expr expr);

    Table.Join.Expr getStoredExpr(Expr expr);

    ObjectValueClassSet getSet();
}
