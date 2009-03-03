package platform.server.data.query;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.where.Where;

import java.util.HashMap;
import java.util.Map;

class AndJoinQuery {

    AndJoinQuery(Where iJoinWhere,Where iQueryWhere, String iAlias) {
        joinWhere = iJoinWhere;
        queryWhere = iQueryWhere;
        alias = iAlias;

        where = joinWhere.and(queryWhere);
    }

    Where joinWhere;
    Where queryWhere;
    Where where;
    String alias;
    Map<String, SourceExpr> properties = new HashMap<String, SourceExpr>();
}
