package platform.server.classes.sets;

import platform.base.ImmutableObject;
import platform.base.SFunctionSet;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.AddValue;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.SimpleAddValue;
import platform.server.classes.*;
import platform.server.data.expr.query.Stat;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;

import java.util.Iterator;
import java.util.Set;

// IMMUTABLE
public class OrObjectClassSet extends ImmutableObject implements OrClassSet, AndClassSet {

    public final UpClassSet up;
    public final ImSet<ConcreteCustomClass> set;
    public final boolean unknown;

    private OrObjectClassSet(UpClassSet up, ImSet<ConcreteCustomClass> set, boolean unknown) {
        this.up = up;
        this.set = set;
        this.unknown = unknown;
    }

    public OrObjectClassSet(UpClassSet up) {
        this(up, SetFact.<ConcreteCustomClass>EMPTY(),false);
    }

    public OrObjectClassSet(ConcreteCustomClass customClass) {
        this(UpClassSet.FALSE, SetFact.singleton(customClass),false);
    }

    public OrObjectClassSet() {
        this(UpClassSet.FALSE, SetFact.<ConcreteCustomClass>EMPTY(),true);
    }

    private OrObjectClassSet(boolean isFalse) {
        this(UpClassSet.FALSE, SetFact.<ConcreteCustomClass>EMPTY(),false);
    }
    public final static OrObjectClassSet FALSE = new OrObjectClassSet(true);

    // добавляет отфильтровывая up'ы
    private static void addAll(MSet<ConcreteCustomClass> mTo, ImSet<ConcreteCustomClass> set, UpClassSet up, boolean has) {
        for(int i=0,size=set.size();i<size;i++) {
            ConcreteCustomClass nodeSet = set.get(i);
            if(up.has(nodeSet)==has)
                mTo.add(nodeSet);
        }
    }

    private static void addAll(MSet<ConcreteCustomClass> mTo, ImSet<ConcreteCustomClass> set, ImSet<ConcreteCustomClass> and) {
        for(int i=0,size=set.size();i<size;i++) {
            ConcreteCustomClass nodeSet = set.get(i);
            if(and.contains(nodeSet))
                mTo.add(nodeSet);
        }
    }

    private static boolean inSet(ImSet<ConcreteCustomClass> to, UpClassSet up,ImSet<ConcreteCustomClass> set) {
        for(int i=0,size=to.size();i<size;i++)
            if(!up.has(to.get(i)) && !set.contains(to.get(i))) return false;
        return true;
    }

    private static ImSet<ConcreteCustomClass> remove(ImSet<ConcreteCustomClass> to, final UpClassSet up) {
        return to.filterFn(new SFunctionSet<ConcreteCustomClass>() {
            public boolean contains(ConcreteCustomClass nodeSet) {
                return !up.has(nodeSet);
            }
        });
    }

    public OrObjectClassSet or(OrClassSet node) {
        return or((OrObjectClassSet)node);
    }

    public AndClassSet[] getAnd() {
        int size = set.size();
        AndClassSet[] result = new AndClassSet[size+(up.isEmpty()?0:1)+(unknown?1:0)]; int r=0;
        for(int i=0;i<size;i++)
            result[r++] = set.get(i);
        if(!up.isEmpty())
            result[r++] = up;
        if(unknown)
            result[r] = new OrObjectClassSet(); // бред, конечно но может и прокатит
        return result;
    }

    public OrObjectClassSet or(OrObjectClassSet node) {
        // or'им Up'ы, or'им Set'ы после чего вырезаем из Set'а все кто есть в Up'ах

        MSet<ConcreteCustomClass> mAddSet = SetFact.mSet();
        addAll(mAddSet, set, node.up, false);
        addAll(mAddSet, node.set, up, false);
        ImSet<ConcreteCustomClass> orSet = mAddSet.immutable();
        UpClassSet orUp = up.add(node.up);

        while(true) {
            UpClassSet parentSet = null;
            for(int i=0,size=orSet.size();i<size;i++) {
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
                orSet = remove(orSet, parentSet);
                orUp = orUp.add(parentSet);
            }
        }
    }

    public OrObjectClassSet and(OrClassSet node) {
        return and((OrObjectClassSet)node);
    }

    public OrObjectClassSet and(OrObjectClassSet node) {
        // or'им Up'ы, or'им Set'ы после чего вырезаем из Set'а все кто есть в Up'ах

        MSet<ConcreteCustomClass> mAndSet = SetFact.mSet();
        addAll(mAndSet, set, node.set);
        addAll(mAndSet, set, node.up, true);
        addAll(mAndSet, node.set, up, true);
        return new OrObjectClassSet(up.intersect(node.up),mAndSet.immutable(),unknown && node.unknown);
    }
    
    public boolean isEmpty() {
        return set.isEmpty() && up.isFalse() && !unknown;
    }

    public boolean containsAll(OrClassSet node) { // ради этого метода все и делается
        OrObjectClassSet objectNode = ((OrObjectClassSet)node);
        return !(objectNode.unknown && !unknown) && inSet(objectNode.set, up, set) && objectNode.up.inSet(up, set);
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

        Set<CustomClass> commonParents = SetFact.mAddRemoveSet(SetFact.toExclSet(up.getCommonClasses()).addExcl(set));
        while(commonParents.size()>1) {
            Iterator<CustomClass> i = commonParents.iterator();
            CustomClass first = i.next(); i.remove();
            CustomClass second = i.next(); i.remove();
            commonParents.addAll(first.commonParents(second).toJavaSet());
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
                if(up.isEmpty() && set.size()==1)
                    return set.single();
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
                return new OrObjectClassSet(baseSet, SetFact.<ConcreteCustomClass>EMPTY(), true);
            else
                return baseSet; 
        }
    }

    private final static AddValue<Object, OrClassSet> addOr = new SimpleAddValue<Object, OrClassSet>() {
        public OrClassSet addValue(Object key, OrClassSet prevValue, OrClassSet newValue) {
            return prevValue.or(newValue);
        }

        public boolean symmetric() {
            return true;
        }
    };
    public static <T> AddValue<T, OrClassSet> addOr() {
        return (AddValue<T, OrClassSet>) addOr;
    }
}
