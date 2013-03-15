package platform.server.classes.sets;

import platform.base.ArrayInstancer;
import platform.server.data.expr.query.Stat;
import platform.server.data.type.Type;

// по сути на Or
public interface AndClassSet {

    AndClassSet getKeepClass();

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
}
