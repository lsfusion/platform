package platform.server.data.query.wheres;

import platform.server.data.query.*;
import platform.server.data.query.exprs.*;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class JoinWhere extends DataWhere implements JoinData {
    Join<?,?> from;

    public JoinWhere(Join iFrom) {
        from =iFrom;
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        from.fillJoins(joins, values);
    }

    public Join getJoin() {
        return from;
    }

    public JoinWheres getInnerJoins() {
        return new JoinWheres(this, Where.TRUE);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(this,andWhere);
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return queryData.get(this);
    }

    public String toString() {
        return "IN JOIN " + from.toString();
    }

    public Where translate(Translator translator) {
        return translator.translate(this);
    }

    protected DataWhereSet getExprFollows() {
        DataWhereSet follows = new DataWhereSet();
        for(SourceExpr expr : from.joins.values())
            follows.addAll(((AndExpr)expr).getFollows());
        return follows;
    }

    public SourceExpr getFJExpr() {
        return new CaseExpr(this, Type.bit.getExpr(true));
    }

    public String getFJString(String exprFJ) {
        return exprFJ + " IS NOT NULL";
    }

    public Where copy() {
        return this;
    }

    // для кэша
    public boolean equals(Where where, Map<ValueExpr, ValueExpr> mapValues, Map<KeyExpr, KeyExpr> mapKeys, MapJoinEquals mapJoins) {
        return where instanceof JoinWhere && mapJoins.equals(this, (JoinWhere) where);
    }

    protected int getHash() {
        return from.hash();
    }
}
