package lsfusion.base.comb;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;

public class SymmetricPairs<T> extends Pairs<T,T> {

    public SymmetricPairs(ImOrderSet<T> from, ImOrderSet<T> to) {
        super(from, to);
    }

    public static <T> SymmetricPairs<T> create(ImSet<T> from, ImSet<T> to) {
        ImSet<T> sameList = SetFact.filter(from, to);
        ImSet<T> diffFrom = SetFact.remove(from, sameList);
        ImSet<T> diffTo = SetFact.remove(to, sameList);
        return new SymmetricPairs<>(sameList.toOrderSet().addOrderExcl(diffFrom.toOrderSet()), sameList.toOrderSet().addOrderExcl(diffTo.toOrderSet()));
    }
}
