package lsfusion.server.logics.action.session;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.type.Type;

public class SingleKeyTableUsage<P> extends SessionTableUsage<String, P> {

    public SingleKeyTableUsage(String debugInfo, final Type keyType, ImOrderSet<P> properties, Type.Getter<P> propertyType) {
        super(debugInfo, SetFact.singletonOrder("key"), properties, new Type.Getter<String>() {
            public Type getType(String key) {
                return keyType;
            }
        }, propertyType);
    }

    public Join<P> join(Expr key) {
        return join(MapFact.singleton("key", key));
    }
}
