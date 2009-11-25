package platform.server.data.query;

import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

import java.util.HashMap;
import java.util.Map;

class AndJoinQuery {

    AndJoinQuery(InnerWhere iInner,Where iWhere, String iAlias) {
        inner = iInner;
        alias = iAlias;

        where = iWhere;
    }

    final InnerWhere inner;
    final Where where;
    final String alias;
    final Map<String, Expr> properties = new HashMap<String, Expr>();
}
