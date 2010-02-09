package platform.server.data.where;

import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWheres;
import platform.server.data.query.HashContext;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.SourceJoin;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;

import java.util.Map;

public interface Where<Not extends Where> extends SourceJoin {

    Where followFalse(Where falseWhere);
    
    public abstract <K> Map<K, Expr> followTrue(Map<K,? extends Expr> map);

    // внутренние
    Where innerFollowFalse(Where falseWhere, boolean sureNotTrue);
    boolean checkTrue();
    boolean directMeansFrom(AndObjectWhere where);

    Not not();

    boolean isTrue();
    boolean isFalse();

    Where and(Where where);
    Where or(Where where);
    Where andMeans(Where where); // чисто для means 
    Where orMeans(Where where); // чисто для means
    boolean means(Where where);

    AndObjectWhere[] getAnd();
    OrObjectWhere[] getOr();

    Map<BaseExpr,ValueExpr> getExprValues();
    Map<KeyExpr, BaseExpr> getKeyExprs();

    ObjectWhereSet getObjects();

    // получает where такой что this => result, а также result.getObjects() not linked с decompose
    // также получает objects not linked с decompose, (потому как при сокращении могут вырезаться некоторые)
    Where decompose(ObjectWhereSet decompose, ObjectWhereSet objects);

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

    public abstract Where translateDirect(KeyTranslator translator);
    public abstract Where translateQuery(QueryTranslator translator);
}
