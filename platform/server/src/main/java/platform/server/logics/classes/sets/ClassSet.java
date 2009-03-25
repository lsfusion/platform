package platform.server.logics.classes.sets;

import platform.base.CollectionExtend;
import platform.server.data.types.Type;
import platform.server.logics.classes.RemoteClass;

import java.util.*;

// по сути на Or
public class ClassSet {
    UpClassSet up;
    Set<RemoteClass> set;

    ClassSet(UpClassSet iUp, Set<RemoteClass> iSet) {
        up = iUp;
        set = iSet;
    }

    public ClassSet() {
        up = new UpClassSet();
        set = new HashSet<RemoteClass>();
    }

    public ClassSet(RemoteClass node) {
        this(new UpClassSet(), Collections.singleton(node));
    }

    public static ClassSet getUp(RemoteClass node) {
        return new ClassSet(new UpClassSet(Collections.singleton(node)),new HashSet<RemoteClass>());
    }

    ClassSet and(ClassSet node) {
        // Up*Node.Up OR (Up*Node.Set+Set*Node.Up+Set*Node.Set) (легко доказать что второе не пересек. с первым)
        Set<RemoteClass> AndSet = CollectionExtend.intersect(set, node.set);
        AndSet.addAll(up.andSet(node.set));
        AndSet.addAll(node.up.andSet(set));
        return new ClassSet(up.and(node.up),AndSet);
    }

    public void or(ClassSet node) {
        // or'им Up'ы, or'им Set'ы после чего вырезаем из Set'а все кто есть в Up'ах
        up.or(node.up);
        set.addAll(node.set);
        for(Iterator<RemoteClass> i = set.iterator();i.hasNext();)
            if(up.has(i.next())) i.remove();
    }

    // входит ли в дерево элемент
    public boolean contains(RemoteClass node) {
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

    public RemoteClass getCommonClass() {
        if(isEmpty())
            throw new RuntimeException("Empty Base Class");

        Set<RemoteClass> commonParents = new HashSet<RemoteClass>(set);
        commonParents.addAll(up);
        while(commonParents.size()>1) {
            Iterator<RemoteClass> i = commonParents.iterator();
            RemoteClass first = i.next(); i.remove();
            RemoteClass second = i.next(); i.remove();
            commonParents.addAll(first.commonParents(second));
        }
        return commonParents.iterator().next();
    }

    public RemoteClass getRandom(Random randomizer) {
        // пока чисто по Up'у
        return CollectionExtend.getRandom(up,randomizer);
    }

    public final static ClassSet universal;
    static {
        universal = getUp(RemoteClass.base);
    }

    public String toString() {
        return (!set.isEmpty()? set.toString():"")+(!up.isEmpty() && !set.isEmpty()?" ":"")+(!up.isEmpty()?"Up:"+ up.toString():"");
    }

    public ClassSet copy() {
        return new ClassSet(new UpClassSet(up),new HashSet<RemoteClass>(set));
    }
}
