package platform.server.where;

import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.classes.where.MeanClassWheres;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.exprs.SourceExpr;

import java.util.Map;

public interface Where<Not extends Where> extends SourceJoin<Where> {

    Where followFalse(Where falseWhere);
    
    public abstract <K> Map<K, SourceExpr> followTrue(Map<K,? extends SourceExpr> map);

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

    int hash();

    int getHeight();

    static Where TRUE = new AndWhere();
    static Where FALSE = new OrWhere();
}
