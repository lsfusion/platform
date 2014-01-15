package lsfusion.server.session;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.Modify;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;

public class SingleKeyPropertyUsage extends SinglePropertyTableUsage<String> {

    public SingleKeyPropertyUsage(final Type keyType, Type propertyType) {
        super(SetFact.singletonOrder("key"), new Type.Getter<String>() {
            public Type getType(String key) {
                return keyType; 
            }
        }, propertyType);
    }

    public ModifyResult modifyRecord(SQLSession session, DataObject keyObject, ObjectValue propertyObject, Modify type) throws SQLException, SQLHandledException {
        return modifyRecord(session, MapFact.singleton("key", keyObject), MapFact.singleton("value", propertyObject), type);
    }
    
    public void writeRows(SQLSession session, KeyExpr key, Expr expr, Where where, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        writeRows(session, new Query<String, String>(MapFact.singletonRev("key", key), expr, "value", where), baseClass, env);
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
        return new PropertyChange<P>(MapFact.singletonRev(propertyInterface, mapKeys.singleValue()), join.getExpr("value"), join.getWhere());
    }

    public ClassChange getChange() {
        KeyExpr key = new KeyExpr("key");
        Join<String> join = join(key);
        return new ClassChange(key, join.getWhere(), join.getExpr("value"));
    }

    public ImCol<ImMap<String, Object>> read(DataSession session, DataObject object) throws SQLException, SQLHandledException {
        return read(session, MapFact.singleton("key", object));
    }
}
