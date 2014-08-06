package lsfusion.server.logics.property.infer;

import lsfusion.base.AddIntersectSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;

//
public class NotNull<T> extends AddIntersectSet<ImSet<T>, NotNull<T>> {

    private NotNull() {
    }
    private final static NotNull EMPTY = new NotNull(SetFact.EMPTY());
    public static <T> NotNull<T> EMPTY() {
        return EMPTY;
    }
    private final static NotNull FALSE = new NotNull();
    public static <T> NotNull<T> FALSE() {
        return FALSE;
    }

    public NotNull(ImSet<T>[] wheres) {
        super(wheres);
    }

    public NotNull(ImSet<T> where) {
        super(where);
    }

    protected ImSet<T>[] intersect(ImSet<T> element1, ImSet<T> element2) {
        return new ImSet[]{element1.merge(element2)};
    }

    protected NotNull<T> createThis(ImSet<T>[] wheres) {
        return new NotNull<T>(wheres);
    }

    protected ImSet<T>[] newArray(int size) {
        return new ImSet[size];
    }

    protected boolean containsAll(ImSet<T> who, ImSet<T> what) {
        return what.containsAll(who);
    }
    
    public NotNull<T> or(NotNull<T> or) {
        return add(or);
    }

    public NotNull<T> and(NotNull<T> or) {
        return intersect(or);
    }

    public <V> NotNull<V> mapRev(ImRevMap<T, V> mapping) {
        ImSet<V>[] result = new ImSet[wheres.length];
        for (int i = 0; i < wheres.length; i++) {
            result[i] = wheres[i].mapRev(mapping);
        }
        return new NotNull<V>(result);
    }
    
    public static <K, V> NotNull<V> nullMapRev(NotNull<K> map, ImRevMap<K, V> mapping) {
        return map == null ? null : map.mapRev(mapping);
    }

    public NotNull<T> remove(ImSet<? extends T> remove) {
        NotNull<T> result = NotNull.FALSE();
        for(ImSet<T> where : wheres)
            result = result.add(new NotNull<T>(where.remove(remove)));
        return result;
    }

    public static <T> NotNull<T> nullRemove(NotNull<T> set, ImSet<? extends T> remove) {
        return set == null ? null : set.remove(remove);
    }

    public NotNull<T> filter(ImSet<? extends T> set) {
        NotNull<T> result = NotNull.FALSE();
        for(ImSet<T> where : wheres)
            result = result.add(new NotNull<T>(where.filter(set)));
        return result;
    }

    public static <T> NotNull<T> nullFilter(NotNull<T> set, ImSet<? extends T> remove) {
        return set == null ? null : set.filter(remove);
    }
    
    public boolean isNotNull(ImSet<T> interfaces) {
        for(ImSet<T> where : wheres)
            if(!where.containsAll(interfaces))
                return false;
        return true;
    }
    
    public ImSet<T>[] getArray() {
        return wheres;
    }
}
