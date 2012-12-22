package platform.server.session;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.server.data.Modify;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;

public class SingleKeyNoPropertyUsage extends NoPropertyTableUsage<String> {

    public SingleKeyNoPropertyUsage(final Type keyType) {
        super(SetFact.singletonOrder("key"), new Type.Getter<String>() {
            public Type getType(String key) {
                return keyType;
            }
        });
    }

    public Where getWhere(Expr expr) {
        return getWhere(MapFact.singleton("key", expr));
    }

    public void modifyRecord(SQLSession session, DataObject keyObject, Modify type) throws SQLException {
        modifyRecord(session, MapFact.singleton("key", keyObject), MapFact.<Object, ObjectValue>EMPTY(), type);
    }

}
