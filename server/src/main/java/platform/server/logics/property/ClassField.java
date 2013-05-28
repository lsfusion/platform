package platform.server.logics.property;

import platform.server.classes.ObjectValueClassSet;
import platform.server.data.Table;
import platform.server.data.expr.Expr;

public interface ClassField {

    Table.Join.Expr getStoredExpr(Expr expr);

    ObjectValueClassSet getSet();
}
