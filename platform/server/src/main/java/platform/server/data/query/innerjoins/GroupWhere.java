package platform.server.data.query.innerjoins;

import org.antlr.analysis.SemanticContext;
import platform.base.BaseUtils;
import platform.base.TwinImmutableInterface;
import platform.base.TwinImmutableObject;
import platform.server.caches.ManualLazy;
import platform.server.caches.PackInterface;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public abstract class GroupWhere<T extends GroupWhere<T>> extends TwinImmutableObject implements PackInterface<T> {
    
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
    public boolean twins(TwinImmutableInterface o) {
        return keyEqual.equals(((GroupWhere)o).keyEqual) && where.equals(((GroupWhere)o).where);
    }

    @Override
    public int immutableHashCode() {
        return 31 * keyEqual.hashCode() + where.hashCode();
    }
    
    public static <T extends GroupWhere> List<T> sort(Collection<T> joins) {
        return BaseUtils.sort(joins, GroupWhere.<T>comparator());
    }
}
