package platform.server.data.query;

import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.Where;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SourceJoin {

    String getSource(Map<QueryData, String> queryData, SQLSyntax syntax);

    <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values);
//    void fillJoins(List<? extends Join> Joins);
    void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere);
}
