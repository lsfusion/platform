package platform.server.data.query;

import platform.server.data.expr.Expr;
import platform.server.data.query.innerjoins.GroupJoinsWhere;

import java.util.HashMap;
import java.util.Map;

public class AndJoinQuery {

    public final GroupJoinsWhere innerSelect;
    public final String alias;

    AndJoinQuery(GroupJoinsWhere innerSelect, String alias) {
        this.innerSelect = innerSelect;
        this.alias = alias;
    }

    final Map<String, Expr> properties = new HashMap<String, Expr>();
}
