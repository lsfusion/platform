package lsfusion.server.logics.property;

import lsfusion.server.classes.ObjectValueClassSet;
import lsfusion.server.data.Table;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.InnerFollows;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.logics.table.ImplementTable;

public interface ClassField extends InnerFollows.Field {

    Table.Join.Expr getInconsistentExpr(Expr expr);

    Table.Join.Expr getStoredExpr(Expr expr);

    ObjectValueClassSet getObjectSet();

    ImplementTable getTable();

    CalcProperty getProperty();
}
