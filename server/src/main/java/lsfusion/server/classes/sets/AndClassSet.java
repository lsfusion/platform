package lsfusion.server.classes.sets;

import lsfusion.base.ArrayInstancer;
import lsfusion.server.classes.ValueClassSet;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.type.Type;

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
    
    public final static ArrayInstancer<AndClassSet> arrayInstancer = new ArrayInstancer<AndClassSet>() {
        public AndClassSet[] newArray(int size) {
            return new AndClassSet[size];
        }
    };

    ValueClassSet getValueClassSet();
    
    String getCanonicalName();
}
