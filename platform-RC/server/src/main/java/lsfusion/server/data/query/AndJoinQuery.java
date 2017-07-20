package lsfusion.server.data.query;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.innerjoins.GroupJoinsWhere;

public class AndJoinQuery {

    public final GroupJoinsWhere innerSelect;
    public final String alias;

    AndJoinQuery(GroupJoinsWhere innerSelect, String alias) {
        this.innerSelect = innerSelect;
        this.alias = alias;
    }

    final MExclMap<String, Expr> properties = MapFact.mExclMap();
}
