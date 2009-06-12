package platform.server.data.query;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.where.Where;

import java.util.HashMap;
import java.util.Map;

class AndJoinQuery {

    AndJoinQuery(Where iJoinWhere,Where iWhere, String iAlias) {
        joinWhere = iJoinWhere;
        alias = iAlias;

        where = iWhere;
    }

    final Where joinWhere;
    final Where where;
    final String alias;
    final Map<String, SourceExpr> properties = new HashMap<String, SourceExpr>();
}
