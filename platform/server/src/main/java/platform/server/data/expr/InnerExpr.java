package platform.server.data.expr;

import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.JoinData;
import platform.server.data.translator.TranslateExprLazy;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

import java.util.HashMap;
import java.util.Map;

@TranslateExprLazy
public abstract class InnerExpr extends NotNullExpr implements JoinData {

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        joins.add(this, andWhere);
    }

    public Expr getFJExpr() {
        return this;
    }

    public String getFJString(String exprFJ) {
        return exprFJ;
    }

    public VariableExprSet calculateExprFollows() {
        VariableExprSet result = new VariableExprSet(getJoinFollows());
        result.add(this);
        return result;
    }

    public abstract VariableExprSet getJoinFollows();

    public abstract class NotNull extends NotNullExpr.NotNull {

        protected DataWhereSet calculateFollows() {
            return new DataWhereSet(getJoinFollows());
        }
    }

    public static <K> VariableExprSet getExprFollows(Map<K, BaseExpr> map) {
        return new VariableExprSet(map.values());
    }
}
