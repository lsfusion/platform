package lsfusion.server.form.entity;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.form.instance.Instantiable;
import lsfusion.server.form.instance.OrderInstance;
import lsfusion.server.session.Modifier;

import java.sql.SQLException;

public interface OrderEntity<T extends OrderInstance> extends Instantiable<T>, CompareEntity {

    GroupObjectEntity getApplyObject(FormEntity formEntity, ImSet<GroupObjectEntity> excludeGroupObjects);

    Expr getEntityExpr(ImMap<ObjectEntity, ? extends Expr> mapExprs, Modifier modifier) throws SQLException, SQLHandledException;
}
