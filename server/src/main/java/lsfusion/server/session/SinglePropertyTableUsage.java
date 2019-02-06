package lsfusion.server.session;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.where.extra.CompareWhere;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;

public class SinglePropertyTableUsage<K> extends SessionTableUsage<K, String> {

    public SinglePropertyTableUsage(String debugInfo, ImOrderSet<K> keys, Type.Getter<K> keyType, final Type propertyType) {
        super(debugInfo, keys, SetFact.singletonOrder("value"), keyType, new Type.Getter<String>() {
            public Type getType(String key) {
                return propertyType;
            }
        });
    }

    public void updateAdded(SQLSession sql, BaseClass baseClass, Pair<Long, Long>[] shifts, OperationOwner owner) throws SQLException, SQLHandledException {
        updateAdded(sql, baseClass, "value", shifts, owner);
    }

    public void writeRows(SQLSession session, OperationOwner opOwner, ImMap<ImMap<K,DataObject>, ObjectValue> writeRows) throws SQLException, SQLHandledException {
        writeRows(session, writeRows.mapValues(new GetValue<ImMap<String, ObjectValue>, ObjectValue>() {
            public ImMap<String, ObjectValue> getMapValue(ObjectValue value) {
                return MapFact.singleton("value", value);
            }
        }), opOwner);
    }

    public static <P extends PropertyInterface> PropertyChange<P> getChange(SinglePropertyTableUsage<P> table) {
        ImRevMap<P, KeyExpr> mapKeys = table.getMapKeys();
        Join<String> join = table.join(mapKeys);
        return new PropertyChange<>(mapKeys, join.getExpr("value"), join.getWhere());
    }
}
