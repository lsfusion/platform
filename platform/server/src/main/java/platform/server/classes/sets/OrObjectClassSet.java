package platform.server.classes.sets;

import platform.base.BaseUtils;
import platform.server.classes.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class OrObjectClassSet implements OrClassSet {

    public final UpClassSet up;
    public final ConcreteCustomClassSet set;
    public final boolean unknown;

    private OrObjectClassSet(UpClassSet up, ConcreteCustomClassSet set, boolean unknown) {
        this.up = up;
        this.set = set;
        this.unknown = unknown;
    }

    public OrObjectClassSet(UpClassSet up) {
        this(up,new ConcreteCustomClassSet(),false);
    }

    public OrObjectClassSet(ConcreteCustomClass customClass) {
        this(UpClassSet.FALSE,new ConcreteCustomClassSet(customClass),false);
    }

    public OrObjectClassSet() {
        this(UpClassSet.FALSE,new ConcreteCustomClassSet(),true);
    }

    private OrObjectClassSet(boolean isFalse) {
        this(UpClassSet.FALSE,new ConcreteCustomClassSet(),false);
    }
    public final static OrObjectClassSet FALSE = new OrObjectClassSet(true);

    public OrObjectClassSet or(OrClassSet node) {
        return or((OrObjectClassSet)node);
    }

    public OrObjectClassSet or(OrObjectClassSet node) {
        // or'им Up'ы, or'им Set'ы после чего вырезаем из Set'а все кто есть в Up'ах
        UpClassSet orUp = up.add(node.up);

        ConcreteCustomClassSet orSet = new ConcreteCustomClassSet();
        orSet.addAll(set,node.up);
        orSet.addAll(node.set,up);
        return new OrObjectClassSet(orUp,orSet,unknown || node.unknown);
    }

    public boolean isEmpty() {
        return set.isEmpty() && up.isFalse() && !unknown;
    }

    public boolean containsAll(OrClassSet node) { // ради этого метода все и делается
        OrObjectClassSet objectNode = ((OrObjectClassSet)node);
        return !(objectNode.unknown && !unknown) && objectNode.set.inSet(up, set) && objectNode.up.inSet(up, set);
    }

    public boolean equals(Object o) {
        return this == o || o instanceof OrObjectClassSet && ((OrObjectClassSet)o).containsAll(this) && containsAll((OrObjectClassSet)o);
    }

    public int hashCode() {
        return 1;
    }

    public String toString() {
        return set+(!up.isFalse() && !set.isEmpty()?" ":"")+(!up.isFalse()?"Up:"+ up.toString():"")+(!up.isFalse() || !set.isEmpty()?" ":"")+(unknown?"unknown":"");
    }

    public ValueClass getCommonClass() {
        assert (!isEmpty());
        assert !unknown;

        Set<CustomClass> commonParents = new HashSet<CustomClass>(BaseUtils.merge(Arrays.asList(up.getCommonClasses()),set.toCollection()));
        while(commonParents.size()>1) {
            Iterator<CustomClass> i = commonParents.iterator();
            CustomClass first = i.next(); i.remove();
            CustomClass second = i.next(); i.remove();
            commonParents.addAll(first.commonParents(second).toCollection());
        }
        return commonParents.iterator().next();
    }

    // получает конкретный класс если он один
    public ConcreteObjectClass getSingleClass(BaseClass baseClass) {
        if(unknown) {
            if(up.isEmpty() && set.isEmpty())
                return baseClass.unknown;
        } else {
            if(!set.isEmpty()) {
                ConcreteCustomClass single;
                if(up.isEmpty() && (single = set.getSingle())!=null)
                    return single;
            } else
                return up.getSingleClass();
        }
        return null;
    }
}
