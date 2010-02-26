package platform.server.session;

import platform.server.data.expr.ValueExpr;

import java.util.Map;

public class SessionChanges extends Changes<SessionChanges> {

    public SessionChanges() {
    }

    public SessionChanges(SessionChanges changes) {
        super(changes);
    }

    private SessionChanges(SessionChanges changes, Map<ValueExpr, ValueExpr> mapValues) {
        super(changes, mapValues);
    }

    public SessionChanges translate(Map<ValueExpr, ValueExpr> mapValues) {
        return new SessionChanges(this,mapValues);
    }
}
