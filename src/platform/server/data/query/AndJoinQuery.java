package platform.server.data.query;

import java.util.Map;
import java.util.HashMap;

import platform.server.where.Where;
import platform.server.data.query.exprs.SourceExpr;

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
