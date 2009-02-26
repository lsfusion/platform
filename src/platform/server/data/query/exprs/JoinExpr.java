package platform.server.data.query.exprs;

import platform.server.data.query.Join;
import platform.server.data.query.JoinData;
import platform.server.data.query.QueryData;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.query.wheres.NotNullWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class JoinExpr<J,U> extends ObjectExpr implements JoinData {
    U property;
    public Join<J,U> from;
    NotNullWhere notNull;

    public JoinExpr(Join<J,U> iFrom,U iProperty) {
        from = iFrom;
        property = iProperty;
        notNull = new NotNullWhere(this);
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        from.fillJoins(joins, values);
    }

    public Join getJoin() {
        return from;
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(this, andWhere);
    }

    // для fillSingleSelect'а
    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return queryData.get(this);
    }

    public String toString() {
        return from.toString() + "." + property;
    }

    public Type getType() {
        return from.source.getType(property);
    }

    // возвращает Where без следствий
    Where calculateWhere() {
        return notNull;
    }

    boolean follow(DataWhere Where) {
        return Where== notNull || from.inJoin.follow(Where);
    }
    public DataWhereSet getFollows() {
        return notNull.getFollows();
    }

    public SourceExpr getFJExpr() {
        return this;
    }

    public String getFJString(String exprFJ) {
        return exprFJ;
    }

    int getHash() {
        return from.hash()*31+ from.source.hashProperty(property);
    }
}
