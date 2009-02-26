package platform.server.data.query.exprs;

import platform.server.data.query.Join;
import platform.server.data.query.JoinData;
import platform.server.data.query.QueryData;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.Where;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class KeyExpr extends ObjectExpr implements QueryData {

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return queryData.get(this);
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public Type getType() {
        return Type.object;
    }

    // возвращает Where без следствий
    Where calculateWhere() {
        return Where.TRUE;
    }
}
