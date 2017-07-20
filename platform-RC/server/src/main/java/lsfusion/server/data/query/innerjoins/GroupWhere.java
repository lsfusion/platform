package lsfusion.server.data.query.innerjoins;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.data.where.Where;

import java.util.Comparator;

public abstract class GroupWhere<T extends GroupWhere<T>> extends TwinImmutableObject {
    
    public final KeyEqual keyEqual;
    public final Where where;

    public GroupWhere(KeyEqual keyEqual, Where where) {
        this.keyEqual = keyEqual;
        this.where = where;
    }

    public long getComplexity(boolean outer) {
        return where.getComplexity(outer);
    }
    private static final Comparator<GroupWhere> comparator = new Comparator<GroupWhere>() {
        public int compare(GroupWhere o1, GroupWhere o2) {
            long compl1 = o1.getComplexity(true);
            long compl2 = o2.getComplexity(true);
            if(compl1 > compl2)
                return 1;
            if(compl1 < compl2)
                return -1;
            return 0;
        }
    };
    private static <K extends GroupWhere> Comparator<K> comparator() {
        return (Comparator<K>) comparator;
    }

    private Where fullWhere;
    @ManualLazy
    public Where getFullWhere() {
        if(fullWhere==null)
            fullWhere = where.and(keyEqual.getWhere());
        return fullWhere;
    }

    @Override
    public boolean calcTwins(TwinImmutableObject o) {
        return keyEqual.equals(((GroupWhere)o).keyEqual) && where.equals(((GroupWhere)o).where);
    }

    @Override
    public int immutableHashCode() {
        return 31 * keyEqual.hashCode() + where.hashCode();
    }
    
    public static <T extends GroupWhere> ImList<T> sort(ImCol<T> joins) {
        return joins.sort(GroupWhere.<T>comparator());
    }
}
