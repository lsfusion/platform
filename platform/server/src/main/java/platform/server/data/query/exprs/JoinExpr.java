package platform.server.data.query.exprs;

import platform.server.data.query.Join;
import platform.server.data.query.JoinData;
import platform.server.data.query.QueryData;
import platform.server.data.query.MapJoinEquals;
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

import net.jcip.annotations.Immutable;


public class JoinExpr<J,U> extends ObjectExpr implements JoinData {
    private final U property;
    public final Join<J,U> from;
    private final NotNullWhere notNull;

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
    protected Where calculateWhere() {
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

    protected int getHash() {
        return from.hash()*31+ from.source.hashProperty(property);
    }

    // для кэша
    public boolean equals(SourceExpr expr, Map<ValueExpr, ValueExpr> mapValues, Map<KeyExpr, KeyExpr> mapKeys, MapJoinEquals mapJoins) {
        return expr instanceof JoinExpr && mapJoins.equals(this, (JoinExpr) expr);
    }
}
