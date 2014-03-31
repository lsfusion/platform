package lsfusion.server.session;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.server.data.Modify;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;

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

    public void modifyRecord(SQLSession session, DataObject keyObject, Modify type, OperationOwner owner) throws SQLException, SQLHandledException {
        modifyRecord(session, MapFact.singleton("key", keyObject), MapFact.<Object, ObjectValue>EMPTY(), type, owner);
    }

}
