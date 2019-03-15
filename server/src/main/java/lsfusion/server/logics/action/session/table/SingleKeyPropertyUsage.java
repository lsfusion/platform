package lsfusion.server.logics.action.session.table;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.modify.Modify;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.change.ModifyResult;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.classes.change.ClassChange;
import lsfusion.server.logics.classes.user.set.ObjectClassSet;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class SingleKeyPropertyUsage extends SinglePropertyTableUsage<String> {

    public SingleKeyPropertyUsage(String debugInfo, final Type keyType, Type propertyType) {
        super(debugInfo, SetFact.singletonOrder("key"), new Type.Getter<String>() {
            public Type getType(String key) {
                return keyType; 
            }
        }, propertyType);
    }

    public ModifyResult modifyRecord(SQLSession session, DataObject keyObject, ObjectValue propertyObject, Modify type, OperationOwner owner) throws SQLException, SQLHandledException {
        return modifyRecord(session, MapFact.singleton("key", keyObject), MapFact.singleton("value", propertyObject), type, owner);
    }

    public Join<String> join(Expr expr) {
        return join(MapFact.singleton("key", expr));
    }

    public Where getWhere(Expr expr) {
        return getWhere(MapFact.singleton("key", expr));
    }

    public Expr getExpr(Expr expr) {
        return join(MapFact.singleton("key", expr)).getExpr("value");
    }

    public static <P extends PropertyInterface> PropertyChange<P> getChange(SingleKeyPropertyUsage table, P propertyInterface) {
        ImRevMap<String, KeyExpr> mapKeys = table.getMapKeys();
        Join<String> join = table.join(mapKeys);
        return new PropertyChange<>(MapFact.singletonRev(propertyInterface, mapKeys.singleValue()), join.getExpr("value"), join.getWhere());
    }

    public ClassChange getChange() {
        KeyExpr key = new KeyExpr("key");
        Join<String> join = join(key);
        return new ClassChange(key, join.getWhere(), join.getExpr("value"));
    }

    public void writeRows(SQLSession session, OperationOwner opOwner, ImMap<DataObject,ObjectValue> writeRows) throws SQLException, SQLHandledException {
        writeRows(session, writeRows.mapKeyValues(new GetValue<ImMap<String, DataObject>, DataObject>() {
            public ImMap<String, DataObject> getMapValue(DataObject value) {
                return MapFact.singleton("key", value);
            }
        }, new GetValue<ImMap<String, ObjectValue>, ObjectValue>() {
            public ImMap<String, ObjectValue> getMapValue(ObjectValue value) {
                return MapFact.singleton("value", value);
            }
        }), opOwner);
    }

    public ImCol<ImMap<String, Object>> read(SQLSession session, QueryEnvironment env, DataObject object) throws SQLException, SQLHandledException {
        return read(session, env, MapFact.singleton("key", object));
    }

    public ObjectClassSet getClasses() {
        return (ObjectClassSet)getClassWhere(MapFact.singletonRev("key", "key")).getCommonClass("key");
    }
}
