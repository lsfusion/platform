package platform.server.data.query.exprs;

import platform.server.data.query.Join;
import platform.server.data.query.JoinData;
import platform.server.data.query.QueryData;
import platform.server.data.query.MapJoinEquals;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.Where;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class NullExpr extends ObjectExpr {

    Type type;
    public NullExpr(Type iType) {
        type = iType;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return Type.NULL;
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public String toString() {
        return Type.NULL;
    }

    public Type getType() {
        return type;
    }

    // возвращает Where на notNull
    Where calculateWhere() {
        return Where.FALSE;
    }

    public boolean equals(Object o) {
        return o instanceof NullExpr;
    }

    protected int getHashCode() {
        return 0;
    }

    // для кэша
    public boolean equals(SourceExpr expr, Map<ValueExpr, ValueExpr> mapValues, Map<KeyExpr, KeyExpr> mapKeys, MapJoinEquals mapJoins) {
        return equals(expr);
    }

    protected int getHash() {
        return hashCode();
    }

    // получает список ExprCase'ов
    public ExprCaseList getCases() {
        return new ExprCaseList();
    }
}
