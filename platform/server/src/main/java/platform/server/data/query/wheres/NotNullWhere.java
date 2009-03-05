package platform.server.data.query.wheres;

import platform.server.data.query.*;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotNullWhere extends DataWhere {

    JoinExpr expr;

    public NotNullWhere(JoinExpr iExpr) {
        expr = iExpr;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return expr.getSource(queryData, syntax) + " IS NOT NULL";
    }

    protected String getNotSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        return expr.getSource(QueryData, Syntax) + " IS NULL";
    }

    public String toString() {
        return expr.toString() + " NOT_NULL";
    }

    public Where translate(Translator translator) {

        SourceExpr transExpr = expr.translate(translator);

        if(transExpr== expr)
            return this;

        return transExpr.getWhere();
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        expr.fillJoins(joins, values);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> Joins, Where AndWhere) {
        expr.fillAndJoinWheres(Joins,AndWhere);
    }

    protected DataWhereSet getExprFollows() {
        return expr.from.inJoin.getFollows();
    }

    public Where getJoinWhere() {
        return expr.from.inJoin; // собсно ради этого все и делается
    }

    public Where getNotJoinWhere() {
//        return super.getNotJoinWhere();
        return Where.TRUE;
    }

    public Where copy() {
        return this;
    }

    public JoinWheres getInnerJoins() {
        return new JoinWheres(expr.from.inJoin,this);
    }

    // для кэша
    public boolean equals(Where Where, Map<ValueExpr, ValueExpr> mapValues, Map<KeyExpr, KeyExpr> mapKeys, MapJoinEquals mapJoins) {
        return Where instanceof NotNullWhere && expr.equals(((NotNullWhere)Where).expr, mapValues, mapKeys, mapJoins);
    }

    protected int getHash() {
        return expr.hash();
    }
}
