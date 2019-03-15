package lsfusion.server.logics.classes.user.set;

import lsfusion.base.lambda.ArrayInstancer;
import lsfusion.server.data.expr.query.stat.Stat;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.ValueClassSet;

// по сути на Or
public interface AndClassSet {

    AndClassSet and(AndClassSet node);

    AndClassSet or(AndClassSet node);

    boolean isEmpty();

    boolean containsAll(AndClassSet node, boolean implicitCast);

    OrClassSet getOr();
    AndClassSet[] getAnd(); // определяет "чистые" And, для and not

    Type getType();
    Stat getTypeStat(boolean forJoin); // использование только в Expr typeStat, и административных функциях работы с таблицей
    
    ArrayInstancer<AndClassSet> arrayInstancer = new ArrayInstancer<AndClassSet>() {
        public AndClassSet[] newArray(int size) {
            return new AndClassSet[size];
        }
    };

    ValueClassSet getValueClassSet();

    ResolveClassSet toResolve();
}
