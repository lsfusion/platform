package platform.server.data.query;

import platform.base.col.MapFact;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.server.data.expr.Expr;
import platform.server.data.query.innerjoins.GroupJoinsWhere;

public class AndJoinQuery {

    public final GroupJoinsWhere innerSelect;
    public final String alias;

    AndJoinQuery(GroupJoinsWhere innerSelect, String alias) {
        this.innerSelect = innerSelect;
        this.alias = alias;
    }

    final MExclMap<String, Expr> properties = MapFact.mExclMap();
}
