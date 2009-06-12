package platform.server.data.classes.where;

import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.*;
import platform.server.data.query.translators.Translator;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;

import java.util.Collection;
import java.util.Map;

public class MeanClassWhere extends DataWhere {

    Collection<? extends AndExpr> exprs;
    ClassExprWhere packWhere;

    MeanClassWhere(Collection<? extends AndExpr> iExprs, ClassExprWhere iPackWhere) {
        exprs = iExprs;
        packWhere = iPackWhere;
    }

    protected DataWhereSet getExprFollows() {
        DataWhereSet follows = new DataWhereSet();
        for(AndExpr expr : exprs)
            follows.addAll(expr.getFollows());
        return follows;
    }

    protected ClassExprWhere calculateClassWhere() {
        return packWhere;
    }

    public int fillContext(Context context, boolean compile) {
        throw new RuntimeException("Not supported");
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        throw new RuntimeException("Not supported");
    }

    protected int getHash() {
        throw new RuntimeException("Not supported");
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        throw new RuntimeException("Not supported");
    }

    public Where translate(Translator translator) {
        throw new RuntimeException("Not supported");
    }

    public InnerJoins getInnerJoins() {
        throw new RuntimeException("Not supported");
    }

    public boolean equals(Where where, MapContext mapContext) {
        throw new RuntimeException("Not supported");
    }
}
