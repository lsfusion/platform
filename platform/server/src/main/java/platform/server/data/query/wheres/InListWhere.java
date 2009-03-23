package platform.server.data.query.wheres;

import platform.server.data.query.*;
import platform.server.data.query.exprs.*;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.*;

public class InListWhere extends DataWhere implements CaseWhere<MapCase<Integer>> {

    AndExpr expr;
    String values;

    public InListWhere(AndExpr iExpr, Collection<Integer> setValues) {
        expr = iExpr;
        values = "";
        for(Integer value : setValues)
            values = (values.length()==0?"": values +',') + value;
    }

    InListWhere(AndExpr iExpr, String iValues) {
        expr = iExpr;
        values = iValues;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return expr.getSource(queryData, syntax) + " IN (" + values + ")";
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы
    protected String getNotSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        String exprSource = expr.getSource(queryData, syntax);
        String notSource = "NOT " + exprSource + " IN (" + values + ")";
        if(expr.getWhere().isTrue())
            return notSource;
        else
            return "(" + exprSource + " IS NULL OR " + notSource + ")";
    }

    public String toString() {
        return expr.toString() + " IN (" + values + ")";
    }

    public Where getCaseWhere(MapCase<Integer> cCase) {
        return new InListWhere(cCase.data.get(0), values);
    }

    public Where translate(Translator translator) {
        Map<Integer, SourceExpr> MapExprs = new HashMap<Integer, SourceExpr>();
        MapExprs.put(0, expr);
        return CaseExpr.translateCase(MapExprs,translator,true, false).getWhere(this);
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        expr.fillJoins(joins, values);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> Joins, Where AndWhere) {
        expr.fillJoinWheres(Joins,AndWhere);
    }

    protected DataWhereSet getExprFollows() {
        return expr.getFollows();
    }

    public Where copy() {
        return new InListWhere(expr, values);
    }

    public JoinWheres getInnerJoins() {
        Where inJoinWhere = Where.TRUE;
        if(expr instanceof JoinExpr)
            inJoinWhere = inJoinWhere.and(((JoinExpr) expr).from.inJoin);
        return new JoinWheres(inJoinWhere,this);
    }

    // для кэша
    public boolean equals(Where where, Map<ValueExpr, ValueExpr> mapValues, Map<KeyExpr, KeyExpr> mapKeys, MapJoinEquals mapJoins) {
        return where instanceof InListWhere && values.equals(((InListWhere)where).values) &&
                expr.equals(((InListWhere)where).expr, mapValues, mapKeys, mapJoins);
    }

    protected int getHash() {
        return expr.hash() + values.hashCode()*31;
    }

    protected int getHashCode() {
        return expr.hashCode() + values.hashCode()*31;
    }

    public boolean equals(Object obj) {
        return this==obj || (obj instanceof InListWhere && expr.equals(((InListWhere)obj).expr) && values.equals(((InListWhere)obj).values));
    }
}
