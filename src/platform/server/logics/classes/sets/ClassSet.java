package platform.server.logics.classes.sets;

import platform.server.logics.classes.DataClass;
import platform.base.CollectionExtend;
import platform.server.data.types.Type;

import java.util.*;

// по сути на Or
public class ClassSet {
    UpClassSet up;
    Set<DataClass> set;

    ClassSet(UpClassSet iUp, Set<DataClass> iSet) {
        up = iUp;
        set = iSet;
    }

    public ClassSet() {
        up = new UpClassSet();
        set = new HashSet<DataClass>();
    }

    public ClassSet(DataClass node) {
        this(new UpClassSet(), Collections.singleton(node));
    }

    public static ClassSet getUp(DataClass node) {
        return new ClassSet(new UpClassSet(Collections.singleton(node)),new HashSet<DataClass>());
    }

    ClassSet and(ClassSet node) {
        // Up*Node.Up OR (Up*Node.Set+Set*Node.Up+Set*Node.Set) (легко доказать что второе не пересек. с первым)
        Set<DataClass> AndSet = CollectionExtend.intersect(set, node.set);
        AndSet.addAll(up.andSet(node.set));
        AndSet.addAll(node.up.andSet(set));
        return new ClassSet(up.and(node.up),AndSet);
    }

    public void or(ClassSet node) {
        // or'им Up'ы, or'им Set'ы после чего вырезаем из Set'а все кто есть в Up'ах
        up.or(node.up);
        set.addAll(node.set);
        for(Iterator<DataClass> i = set.iterator();i.hasNext();)
            if(up.has(i.next())) i.remove();
    }

    // входит ли в дерево элемент
    public boolean contains(DataClass node) {
        return set.contains(node) || up.has(node);
    }
    public boolean isEmpty() {
        return set.isEmpty() && up.isEmpty();
    }

    boolean containsAll(ClassSet node) {
        return and(node).equals(node);
    }
    public boolean intersect(ClassSet node) {
        return !and(node).isEmpty();
    }
    public Type getType() {
        return getCommonClass().getType();
    }

    public boolean equals(Object o) {
        return this == o || o instanceof ClassSet && set.equals(((ClassSet)o).set) && up.equals(((ClassSet)o).up);
    }

    public int hashCode() {
        return 31 * up.hashCode() + set.hashCode();
    }

    public DataClass getCommonClass() {
        if(isEmpty())
            throw new RuntimeException("Empty Base Class");

        Set<DataClass> commonParents = new HashSet<DataClass>(set);
        commonParents.addAll(up);
        while(commonParents.size()>1) {
            Iterator<DataClass> i = commonParents.iterator();
            DataClass first = i.next(); i.remove();
            DataClass second = i.next(); i.remove();
            commonParents.addAll(first.commonParents(second));
        }
        return commonParents.iterator().next();
    }

    public DataClass getRandom(Random randomizer) {
        // пока чисто по Up'у
        return CollectionExtend.getRandom(up,randomizer);
    }

    public static ClassSet universal;
    static {
        universal = getUp(DataClass.base);
    }

    public String toString() {
        return (!set.isEmpty()? set.toString():"")+(!up.isEmpty() && !set.isEmpty()?" ":"")+(!up.isEmpty()?"Up:"+ up.toString():"");
    }

    public ClassSet copy() {
        return new ClassSet(new UpClassSet(up),new HashSet<DataClass>(set));
    }
}
