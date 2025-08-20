package lsfusion.server.logics.action.session.table;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.Join;
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

    private final boolean staticValueExpr;

    public SingleKeyPropertyUsage(String debugInfo, final Type keyType, Type propertyType) {
        this(debugInfo, keyType, propertyType, false);
    }

    // for classes tables it's better to use static exprs for the IsClassExpr checkEquals optimization (important for cases like objectClass(ob) = objectClass(b))
    public SingleKeyPropertyUsage(String debugInfo, final Type keyType, Type propertyType, boolean staticValueExpr) {
        super(debugInfo, SetFact.singletonOrder("key"), key -> keyType, propertyType);

        this.staticValueExpr = staticValueExpr;
    }

    @Override
    protected boolean isStaticValueExpr() {
        return staticValueExpr;
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
        writeRows(session, writeRows.mapKeyValues(value -> MapFact.singleton("key", value), value -> MapFact.singleton("value", value)), opOwner);
    }

    public ImCol<ImMap<String, Object>> read(SQLSession session, QueryEnvironment env, DataObject object) throws SQLException, SQLHandledException {
        return read(session, env, MapFact.singleton("key", object));
    }

    public ObjectClassSet getClasses() {
        return (ObjectClassSet)getClassWhere(MapFact.singletonRev("key", "key")).getCommonClass("key");
    }
}
