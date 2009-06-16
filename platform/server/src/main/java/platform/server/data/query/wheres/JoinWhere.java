package platform.server.data.query.wheres;

import platform.server.data.classes.LogicalClass;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.query.*;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.translators.Translator;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.Map;

public class JoinWhere<J,U> extends DataWhere implements JoinData {
    DataJoin<J,U> from;

    ClassExprWhere joinClassWhere;

    public JoinWhere(DataJoin<J,U> iFrom,ClassExprWhere iJoinClassWhere) {
        from = iFrom;
        joinClassWhere = iJoinClassWhere;
    }

    public int fillContext(Context context, boolean compile) {
        return context.add(from,compile);
    }

    public Join getJoin() {
        return from;
    }

    public InnerJoins getInnerJoins() {
        return new InnerJoins(this,this);
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
            if(expr instanceof AndExpr)
                follows.addAll(((AndExpr)expr).getFollows());
        return follows;
    }

    public SourceExpr getFJExpr() {
        return new CaseExpr(this, new ValueExpr(true, LogicalClass.instance));
    }

    public String getFJString(String exprFJ) {
        return exprFJ + " IS NOT NULL";
    }

    public Where copy() {
        return this;
    }

    // для кэша
    public boolean equals(Where where, MapContext mapContext) {
        return where instanceof JoinWhere && mapContext.equals(this, (JoinWhere) where);
    }

    protected int getHash() {
        return from.hash();
    }

    public ClassExprWhere calculateClassWhere() {
        return joinClassWhere;
    }
}
