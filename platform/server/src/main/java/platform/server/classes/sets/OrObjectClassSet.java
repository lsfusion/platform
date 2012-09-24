package platform.server.classes.sets;

import platform.base.BaseUtils;
import platform.base.QuickMap;
import platform.server.classes.*;
import platform.server.data.expr.query.Stat;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class OrObjectClassSet implements OrClassSet, AndClassSet {

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

    public AndClassSet[] getAnd() {
        AndClassSet[] result = new AndClassSet[set.size+(up.isEmpty()?0:1)+(unknown?1:0)]; int r=0;
        for(int i=0;i<set.size;i++)
            result[r++] = set.get(i);
        if(!up.isEmpty())
            result[r++] = up;
        if(unknown)
            result[r] = new OrObjectClassSet(); // бред, конечно но может и прокатит
        return result;
    }

    public OrObjectClassSet or(OrObjectClassSet node) {
        // or'им Up'ы, or'им Set'ы после чего вырезаем из Set'а все кто есть в Up'ах

        ConcreteCustomClassSet orSet = new ConcreteCustomClassSet();
        orSet.addAll(set,node.up,false);
        orSet.addAll(node.set,up,false);
        UpClassSet orUp = up.add(node.up);

        while(true) {
            UpClassSet parentSet = null;
            for(int i=0;i<orSet.size;i++) {
                for(CustomClass parent : orSet.get(i).parents)
                    if(parent.upInSet(orUp, orSet)) {
                        parentSet = new UpClassSet(parent);
                        break;
                    }
                if(parentSet!=null)
                    break;
            }
            // remove'им orSet
            if(parentSet == null)
                return new OrObjectClassSet(orUp,orSet,unknown || node.unknown);
            else {
                orSet = orSet.remove(parentSet);
                orUp = orUp.add(parentSet);
            }
        }
    }

    public OrObjectClassSet and(OrClassSet node) {
        return and((OrObjectClassSet)node);
    }

    public OrObjectClassSet and(OrObjectClassSet node) {
        // or'им Up'ы, or'им Set'ы после чего вырезаем из Set'а все кто есть в Up'ах
        UpClassSet andUp = up.intersect(node.up);

        ConcreteCustomClassSet andSet = new ConcreteCustomClassSet();
        andSet.addAll(set,node.set);
        andSet.addAll(set,node.up,true);
        andSet.addAll(node.set,up,true);
        return new OrObjectClassSet(andUp,andSet,unknown && node.unknown);
    }
    
    public boolean isEmpty() {
        return set.isEmpty() && up.isFalse() && !unknown;
    }

    public boolean containsAll(OrClassSet node) { // ради этого метода все и делается
        OrObjectClassSet objectNode = ((OrObjectClassSet)node);
        return !(objectNode.unknown && !unknown) && objectNode.set.inSet(up, set) && objectNode.up.inSet(up, set);
    }

    public boolean equals(Object o) {
        return this == o || o instanceof OrObjectClassSet && ((OrObjectClassSet)o).containsAll((OrClassSet)this) && containsAll((OrClassSet)o);
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

        Set<CustomClass> commonParents = new HashSet<CustomClass>(BaseUtils.merge(Arrays.asList(up.getCommonClasses()),set.toSet()));
        while(commonParents.size()>1) {
            Iterator<CustomClass> i = commonParents.iterator();
            CustomClass first = i.next(); i.remove();
            CustomClass second = i.next(); i.remove();
            commonParents.addAll(first.commonParents(second).toSet());
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

    public AndClassSet and(AndClassSet node) {
        return and(node.getOr());
    }

    public AndClassSet or(AndClassSet node) {
        return or(node.getOr());
    }

    public boolean containsAll(AndClassSet node) {
        return containsAll(node.getOr());
    }

    public OrClassSet getOr() {
        return this;
    }

    public Type getType() {
        return ObjectType.instance;
    }
    public Stat getTypeStat() {
        if(up.isEmpty() && set.isEmpty()) {
            if(unknown)
                return Stat.MAX;
            else
                throw new RuntimeException("should not be");
        } else {
            if(up.isEmpty())
                return set.get(0).getTypeStat();
            else
                return up.getTypeStat();
        }
    }

    public static AndClassSet or(ObjectClassSet set1, AndClassSet set2) {
        return set1.getOr().or(set2.getOr());
    }

    public AndClassSet getKeepClass() {
        if(up.isEmpty() && set.isEmpty()) {
            if(unknown)
                return new OrObjectClassSet();
            else
                return OrObjectClassSet.FALSE;
        } else {
            UpClassSet baseSet = (up.isEmpty() ? set.get(0) : up).getBaseClass().getUpSet();
            if(unknown)
                return new OrObjectClassSet(baseSet,new ConcreteCustomClassSet(), true);
            else
                return baseSet; 
        }
    }
}
