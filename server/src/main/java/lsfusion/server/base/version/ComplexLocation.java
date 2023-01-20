package lsfusion.server.base.version;

import lsfusion.base.Pair;

import java.util.List;
import java.util.function.Function;

public abstract class ComplexLocation<T> {

    public abstract Pair<Integer, Integer> getInsertGroup(List<T> list, List<Integer> groupList);

    public abstract boolean isReverseList();

    public abstract <K> ComplexLocation<K> map(Function<T, K> mapNeighbour);

    private static final int LASTGROUP = 1;
    private static final ComplexLocation LAST = new SideComplexLocation(false, LASTGROUP);
    public static <K> ComplexLocation<K> LAST() {
        return LAST;
    }
    protected static final int DEFAULTGROUP = 0;
    private static final ComplexLocation DEFAULT = new SideComplexLocation(false, DEFAULTGROUP);
    public static <K> ComplexLocation<K> DEFAULT() {
        return DEFAULT;
    }

    private static final ComplexLocation FIRST = new SideComplexLocation(true, DEFAULTGROUP);
    public static <K> ComplexLocation<K> FIRST() {
        return FIRST;
    }

    public static <K> ComplexLocation<K> LAST(int group) {
        if(group == DEFAULTGROUP)
            return DEFAULT();
        if(group == LASTGROUP)
            return LAST();

        return new SideComplexLocation<>(false, group); // not used now
    }

    public static <K> ComplexLocation<K> BEFORE(K element) {
        return new NeighbourComplexLocation<>(element, false);
    }
    public static <K> ComplexLocation<K> AFTER(K element) {
        return new NeighbourComplexLocation<>(element, true);
    }
}
