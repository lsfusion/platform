package platform.server.data.where;

import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWheres;
import platform.server.caches.hash.HashContext;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.SourceJoin;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.caches.TranslateContext;
import platform.server.data.translator.QueryTranslator;

import java.util.Map;

public interface Where extends SourceJoin, TranslateContext<Where> {

    Where followFalse(Where falseWhere);
    Where followFalse(Where falseWhere, boolean packExprs);
    Where pack();

    <K> Map<K, Expr> followTrue(Map<K,? extends Expr> map);

    // внутренние
    Where innerFollowFalse(Where falseWhere, boolean sureNotTrue, boolean packExprs);
    boolean checkTrue();
    boolean directMeansFrom(AndObjectWhere where);

    Where not();

    boolean isTrue();
    boolean isFalse();

    Where and(Where where);
    Where or(Where where);
    Where or(Where where, boolean packExprs);
    Where andMeans(Where where); // чисто для means
    Where orMeans(Where where); // чисто для means
    boolean means(Where where);

    AndObjectWhere[] getAnd();
    OrObjectWhere[] getOr();

    Map<BaseExpr,BaseExpr> getExprValues();
    Map<BaseExpr,BaseExpr> getNotExprValues();
    Map<KeyExpr, BaseExpr> getKeyExprs();

    static String TRUE_STRING = "1=1";
    static String FALSE_STRING = "1<>1";

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    abstract public InnerJoins getInnerJoins();
    abstract public MeanClassWheres getMeanClassWheres();

    abstract public ClassExprWhere getClassWhere();

    int hashContext(HashContext hashContext);

    int getHeight();

    static Where TRUE = new AndWhere();
    static Where FALSE = new OrWhere();

    Where translateQuery(QueryTranslator translator);

    public Where map(Map<KeyExpr,? extends Expr> map);
}
