package platform.server.data.query.wheres;

import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.query.*;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.translators.Translator;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.Map;


public class NotNullWhere extends DataWhere {

    private final JoinExpr expr;

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
        return expr.translate(translator).getWhere();
    }

    public int fillContext(Context context, boolean compile) {
        return expr.fillContext(context, compile);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> Joins, Where AndWhere) {
        expr.fillAndJoinWheres(Joins,AndWhere);
    }

    protected DataWhereSet getExprFollows() {
        return ((DataWhere)expr.from.getWhere()).getFollows(); // следует из assertion'а что из JoinExpr => JoinWhere
    }

    public InnerJoins getInnerJoins() {
        return new InnerJoins((JoinWhere)expr.from.getWhere(),this);
    }

    public Where copy() {
        return this;
    }

    // для кэша
    public boolean equals(Where Where, MapContext mapContext) {
        return Where instanceof NotNullWhere && expr.equals(((NotNullWhere)Where).expr, mapContext);
    }

    protected int getHash() {
        return expr.hash();
    }

    public ClassExprWhere calculateClassWhere() {
        return expr.getClassWhere();
    }
}
