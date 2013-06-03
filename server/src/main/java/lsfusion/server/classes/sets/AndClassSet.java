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

    boolean containsAll(AndClassSet node);

    OrClassSet getOr();
    AndClassSet[] getAnd(); // определяет "чистые" And, для and not

    Type getType();
    Stat getTypeStat();
    
    public final static ArrayInstancer<AndClassSet> arrayInstancer = new ArrayInstancer<AndClassSet>() {
        public AndClassSet[] newArray(int size) {
            return new AndClassSet[size];
        }
    };

    ValueClassSet getValueClassSet();
}
