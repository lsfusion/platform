package lsfusion.server.data.query.compile;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.join.where.GroupJoinsWhere;

public class CompileAndQuery {

    public final GroupJoinsWhere innerSelect;
    public final String alias;

    public CompileAndQuery(GroupJoinsWhere innerSelect, String alias) {
        this.innerSelect = innerSelect;
        this.alias = alias;
    }

    public final MExclMap<String, Expr> properties = MapFact.mExclMap();
}
