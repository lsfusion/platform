package platform.server.data.query;

import platform.server.data.expr.Expr;
import platform.server.data.query.innerjoins.InnerSelectJoin;

import java.util.HashMap;
import java.util.Map;

public class AndJoinQuery {

    public final InnerSelectJoin innerSelect;
    public final String alias;

    AndJoinQuery(InnerSelectJoin innerSelect, String alias) {
        this.innerSelect = innerSelect;
        this.alias = alias;
    }

    final Map<String, Expr> properties = new HashMap<String, Expr>();
}
