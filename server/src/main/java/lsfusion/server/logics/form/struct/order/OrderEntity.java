package lsfusion.server.logics.form.struct.order;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.interactive.Instantiable;
import lsfusion.server.logics.form.interactive.instance.order.OrderInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.action.session.Modifier;

import java.sql.SQLException;

public interface OrderEntity<T extends OrderInstance> extends Instantiable<T>, CompareEntity {

    GroupObjectEntity getApplyObject(FormEntity formEntity, ImSet<GroupObjectEntity> excludeGroupObjects);

    Expr getEntityExpr(ImMap<ObjectEntity, ? extends Expr> mapExprs, Modifier modifier) throws SQLException, SQLHandledException;
}
