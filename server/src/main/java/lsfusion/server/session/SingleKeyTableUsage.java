package lsfusion.server.session;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.Modify;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;

import java.sql.SQLException;

public class SingleKeyTableUsage<P> extends SessionTableUsage<String, P> {

    public SingleKeyTableUsage(final Type keyType, ImOrderSet<P> properties, Type.Getter<P> propertyType) {
        super(SetFact.singletonOrder("key"), properties, new Type.Getter<String>() {
            public Type getType(String key) {
                return keyType;
            }
        }, propertyType);
    }

    public Join<P> join(Expr key) {
        return join(MapFact.singleton("key", key));
    }

    public void modifyRecord(SQLSession session, DataObject key, ImMap<P, ObjectValue> propertyValues, Modify type) throws SQLException {
        modifyRecord(session, MapFact.singleton("key", key), propertyValues, type);
    }

}
