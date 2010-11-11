package platform.base;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SymmetricPairs<T> extends Pairs<T,T> {

    public SymmetricPairs(List<T> from, List<T> to) {
        super(from, to);
    }

    public static <T> SymmetricPairs<T> create(Set<? extends T> from, Set<? extends T> to) {
        Set<T> diffTo = new HashSet<T>(to);
        Set<T> diffFrom = new HashSet<T>();
        List<T> sameList = new ArrayList<T>();
        for(T fromElement : from)
            if(diffTo.remove(fromElement))
                sameList.add(fromElement);
            else
                diffFrom.add(fromElement);
        return new SymmetricPairs<T>(BaseUtils.mergeList(sameList, new ArrayList<T>(diffFrom)), BaseUtils.mergeList(sameList, new ArrayList<T>(diffTo)));
    }
}
