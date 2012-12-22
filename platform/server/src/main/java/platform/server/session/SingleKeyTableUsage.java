package platform.server.session;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.server.data.Modify;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

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
