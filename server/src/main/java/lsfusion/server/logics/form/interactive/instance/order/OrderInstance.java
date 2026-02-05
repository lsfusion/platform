package lsfusion.server.logics.form.interactive.instance.order;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingFunction;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.interactive.changed.ReallyChanged;
import lsfusion.server.logics.form.interactive.instance.filter.CompareInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.property.Property;

import java.sql.SQLException;

public interface OrderInstance extends CompareInstance {

    GroupObjectInstance getApplyObject();

    Type getType();

    static ImMap<OrderInstance, Expr> getExprs(ImRevMap<ObjectInstance, KeyExpr> mapKeys, ImOrderMap<OrderInstance, Boolean> orders, Modifier modifier) throws SQLException, SQLHandledException {
        return orders.getMap().mapKeyValuesEx((ThrowingFunction<OrderInstance, Expr, SQLException, SQLHandledException>) value -> value.getExpr(mapKeys, modifier));
    }
}
