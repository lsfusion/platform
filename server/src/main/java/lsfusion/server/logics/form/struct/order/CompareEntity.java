package lsfusion.server.logics.form.struct.order;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

import java.sql.SQLException;

public interface CompareEntity {

    Type getType();

    Expr getEntityExpr(ImMap<ObjectEntity, ? extends Expr> mapExprs, Modifier modifier) throws SQLException, SQLHandledException;
}
