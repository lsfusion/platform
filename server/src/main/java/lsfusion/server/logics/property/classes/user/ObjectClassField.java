package lsfusion.server.logics.property.classes.user;

import lsfusion.server.data.expr.Expr;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.physics.exec.db.table.ImplementTable;

public interface ObjectClassField extends IsClassField {

    Expr getInconsistentExpr(Expr expr);

    Expr getStoredExpr(Expr expr);

    ObjectValueClassSet getObjectSet();

    ImplementTable getTable();

    ClassDataProperty getProperty();
}
