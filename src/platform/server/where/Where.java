package platform.server.where;

import java.util.Collection;
import java.util.Map;

import platform.server.data.query.JoinWheres;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.Translator;
import platform.server.data.query.wheres.JoinWhere;
import platform.server.data.query.exprs.ObjectExpr;

public interface Where<Not extends Where> extends SourceJoin {

    Where followFalse(Where falseWhere);

    // внутренние
    Where siblingsFollow(Where falseWhere);
    boolean checkTrue();
    boolean directMeansFrom(AndObjectWhere where);

    Not not();

    boolean isTrue();
    boolean isFalse();

    Where and(Where where);
    Where or(Where where);
    Where orMeans(Where where); // чисто для means 
    boolean means(Where where);

    AndObjectWhere[] getAnd();
    OrObjectWhere[] getOr();

    ObjectWhereSet getObjects();

    // получает where такой что this => result, а также result.getObjects() not linked с decompose
    // также получает objects not linked с decompose, (потому как при сокращении могут вырезаться некоторые)
    Where decompose(ObjectWhereSet decompose, ObjectWhereSet objects);

    boolean evaluate(Collection<DataWhere> data);

    boolean hashEquals(Where where);

    static String TRUE_STRING = "1=1";
    static String FALSE_STRING = "1<>1";

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    abstract Where translate(Translator translator);

    abstract JoinWheres getInnerJoins();

    abstract boolean equals(Where where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres);

    int hash();

    int getSize();
    int getHeight();

    static Where TRUE = new AndWhere();
    static Where FALSE = new OrWhere();
}
