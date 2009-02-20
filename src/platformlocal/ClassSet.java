package platformlocal;

import java.util.*;

abstract class SubNodeSet<T,S extends SubNodeSet<T,S>> extends HashSet<T> {

    protected SubNodeSet() {
    }

    protected SubNodeSet(Set<T> iNodes) {
        super(iNodes);
    }

    protected SubNodeSet(T node) {
        add(node);
    }

    void or(S set) {
        for(T node : set)
            or(node);
    }
    abstract void or(T orNode);

    S and(S set) {
        S andSet = create(new HashSet<T>());
        for(T node : this)
            andSet.or(set.and(node));
        return andSet;
    }
    S and(T andNode) {
        S andSet = create(new HashSet<T>());
        for(T Node : this) andSet.or(create(and(andNode,Node)));
        return andSet;
    }
    abstract Set<T> and(T andNode,T node);
    abstract S create(Set<T> iNodes);

    public boolean equals(Object o) {
        return this == o || o instanceof SubNodeSet && super.equals(o);
    }
}

abstract class GraphNodeSet<T,S extends GraphNodeSet<T,S>> extends SubNodeSet<T,S> {

    protected GraphNodeSet() {
    }

    protected GraphNodeSet(Set<T> iNodes) {
        super(iNodes);
    }

    boolean has(T checkNode) {
        for(T node : this)
            if(has(checkNode,node)) return true;
        return false;
    }
    abstract boolean has(T orNode,T node);

    void or(T orNode) {
        for(T Node : this)
            if(has(orNode,Node)) return;
        for(Iterator<T> i=iterator();i.hasNext();)
            if(has(i.next(), orNode)) i.remove();
        add(orNode);
    }

    void removeAll(GraphNodeSet<T,S> toRemove) {
        for(Iterator<T> i = iterator();i.hasNext();)
            if(toRemove.has(i.next())) i.remove();
    }

    S excludeAll(GraphNodeSet<T,S> toRemove) {
        S result = create(new HashSet<T>());
        for(T node : this)
            if(!toRemove.has(node)) result.add(node);
        return result;
    }

}

// выше вершин
class UpClassSet extends GraphNodeSet<Class,UpClassSet> {

    UpClassSet() {
    }

    protected UpClassSet(Set<Class> iNodes) {
        super(iNodes);
    }

    boolean has(Class orNode, Class node) {
        return orNode.isParent(node);
    }

    Set<Class> and(Class andNode, Class node) {
        return andNode.commonChilds(node);
    }

    UpClassSet create(Set<Class> iNodes) {
        return new UpClassSet(iNodes);
    }

    Set<Class> andSet(Set<Class> set) {
        Set<Class> result = new HashSet<Class>();
        for(Class node : set)
            if(has(node)) result.add(node);
        return result;
    }

    void removeSet(Set<Class> set) {
        for(Iterator<Class> i = set.iterator();i.hasNext();)
            if(has(i.next())) i.remove();
    }
}

// по сути на Or
class ClassSet {
    UpClassSet up;
    Set<Class> set;

    ClassSet(UpClassSet iUp, Set<Class> iSet) {
        up = iUp;
        set = iSet;
    }

    ClassSet() {
        up = new UpClassSet();
        set = new HashSet<Class>();
    }

    ClassSet(Class node) {
        this(new UpClassSet(),Collections.singleton(node));
    }

    static ClassSet getUp(Class node) {
        return new ClassSet(new UpClassSet(Collections.singleton(node)),new HashSet<Class>());
    }

    ClassSet and(ClassSet node) {
        // Up*Node.Up OR (Up*Node.Set+Set*Node.Up+Set*Node.Set) (легко доказать что второе не пересек. с первым)
        Set<Class> AndSet = CollectionExtend.intersect(set, node.set);
        AndSet.addAll(up.andSet(node.set));
        AndSet.addAll(node.up.andSet(set));
        return new ClassSet(up.and(node.up),AndSet);
    }

    void or(ClassSet node) {
        // or'им Up'ы, or'им Set'ы после чего вырезаем из Set'а все кто есть в Up'ах
        up.or(node.up);
        set.addAll(node.set);
        for(Iterator<Class> i = set.iterator();i.hasNext();)
            if(up.has(i.next())) i.remove();
    }

    // входит ли в дерево элемент
    boolean contains(Class node) {
        return set.contains(node) || up.has(node);
    }
    boolean isEmpty() {
        return set.isEmpty() && up.isEmpty();
    }
    
    boolean containsAll(ClassSet node) {
        return and(node).equals(node);
    }
    boolean intersect(ClassSet node) {
        return !and(node).isEmpty();
    }
    Type getType() {
        return getCommonClass().getType();
    }

    public boolean equals(Object o) {
        return this == o || o instanceof ClassSet && set.equals(((ClassSet)o).set) && up.equals(((ClassSet)o).up);  
    }

    public int hashCode() {
        return 31 * up.hashCode() + set.hashCode();
    }

    Class getCommonClass() {
        if(isEmpty())
            throw new RuntimeException("Empty Base Class");

        Set<Class> commonParents = new HashSet<Class>(set);
        commonParents.addAll(up);
        while(commonParents.size()>1) {
            Iterator<Class> i = commonParents.iterator();
            Class first = i.next(); i.remove();
            Class second = i.next(); i.remove();
            commonParents.addAll(first.commonParents(second));
        }
        return commonParents.iterator().next();
    }

    Class getRandom(Random randomizer) {
        // пока чисто по Up'у
        return CollectionExtend.getRandom(up,randomizer);
    }

    static ClassSet universal;
    static {
        universal = getUp(Class.base);
    }

    public String toString() {
        return (!set.isEmpty()? set.toString():"")+(!up.isEmpty() && !set.isEmpty()?" ":"")+(!up.isEmpty()?"Up:"+ up.toString():"");
    }

    public ClassSet copy() {
        return new ClassSet(new UpClassSet(up),new HashSet<Class>(set));
    }
}

//>;

class InterfaceClass<P extends PropertyInterface> extends HashMap<P,ClassSet> {

    InterfaceClass(Map<P, ObjectValue> keys) {
        for(Map.Entry<P,ObjectValue> key : keys.entrySet())
            put(key.getKey(),new ClassSet(key.getValue().objectClass));
    }

    InterfaceClass() {
    }

    InterfaceClass(P propertyInterface, ClassSet interfaceValue) {
        put(propertyInterface,interfaceValue);
    }

    // когда каждый ClassSet включает в себя все подмн-ва
    public boolean isParent(InterfaceClass<P> node) {
        // здесь строго говоря доджен keySet'ы должны совпадать
        // DEBUG проверка
        if(!keySet().equals(node.keySet()))
            throw new RuntimeException("different interfaces");
        for(Map.Entry<P,ClassSet> propertyInterface : entrySet())
            if(!propertyInterface.getValue().containsAll(node.get(propertyInterface.getKey()))) return false;
        return true;
    }

    public InterfaceClass<P> and(InterfaceClass<P> node) {
        InterfaceClass<P> and = new InterfaceClass<P>();
        and.putAll(node);
        for(Map.Entry<P,ClassSet> propertyInterface : entrySet()) {
            ClassSet andClass = and.get(propertyInterface.getKey());
            if(andClass==null)
                andClass = propertyInterface.getValue();
            else
                andClass = andClass.and(propertyInterface.getValue());
            and.put(propertyInterface.getKey(),andClass);
        }
        return and;
    }

    <V extends PropertyInterface> InterfaceClass<V> map(Map<P,V> mapInterfaces) {
        InterfaceClass<V> result = new InterfaceClass<V>();
        for(Map.Entry<P,V> propertyInterface : mapInterfaces.entrySet())
            result.put(propertyInterface.getValue(),get(propertyInterface.getKey()));
        return result;
    }

    <V extends PropertyInterface> InterfaceClass<V> mapBack(Map<V,P> mapInterfaces) {
        InterfaceClass<V> result = new InterfaceClass<V>();
        for(Map.Entry<V,P> propertyInterface : mapInterfaces.entrySet())
            result.put(propertyInterface.getKey(),get(propertyInterface.getValue()));
        return result;
    }

    public boolean hasEmpty() {
        for(ClassSet set : values())
            if(set.isEmpty()) return true;
        return false;
    }
}

// по сути это Map<InterfaceClass,ClassSet> с определенными ограничениями
// точнее нужна такая структура при которой при одинаковых ClassSet она будет Or'ить InterfaceClassSet'ы - собсно и нужен на самом деле UniMap<InterfaceClassSet,ClassSet>

interface PropertyClass<P extends PropertyInterface> {
    // каких классов могут быть объекты при таких классах на входе !!!! Можно дать больше но никак не меньше
    ClassSet getValueClass(InterfaceClass<P> interfaceImplement);
    // при каких классах мы можем получить хоть один такой класс (при других точно не можем) !!! опять таки лучше больше чем меньше
    InterfaceClassSet<P> getClassSet(ClassSet reqValue);

    // на самом деле нам нужен iterator по <InterfaceClass,ClassSet>
    ValueClassSet<P> getValueClassSet();
}

// по сути Entry для ValueClassSet'а
class ChangeClass<P extends PropertyInterface> {

    InterfaceClassSet<P> interfaceClasses;
    ClassSet value;

    ChangeClass() {
        interfaceClasses = new InterfaceClassSet<P>();
        value = new ClassSet();
    }

    ChangeClass(InterfaceClassSet<P> iInterface, ClassSet iValue) {
        interfaceClasses = iInterface;
        value = iValue;
    }

    ChangeClass(InterfaceClass<P> iInterface, ClassSet iValue) {
        interfaceClasses = new InterfaceClassSet<P>(iInterface);
        value = iValue;
    }

    public ChangeClass(P iInterface, ClassSet iInterfaceValue) {
        interfaceClasses = new InterfaceClassSet<P>(new InterfaceClass<P>(iInterface,iInterfaceValue));
        value = new ClassSet();
    }

    /*    ChangeClass(InterfaceClassSet<P> iInterface) {
          Interface = iInterface;
          Value = new OrClassSet();
      }

      public String toString() {
          return Interface.toString() + " - V - " + Value.toString();
      }

      public ChangeClass(ClassSet iValue) {
          Value = iValue;
          Interface = new InterfaceClassSet<P>();
      }

      public ChangeClass(P iInterface, Class Class) {
          Interface = new InterfaceClassSet<P>(iInterface,Class);
          Value = new OrClassSet();
      }

      public ChangeClass(Class valueClass) {
          Value = new OrClassSet(valueClass);
          Interface = new InterfaceClassSet<P>();
      }
    */
    <V extends PropertyInterface> ChangeClass<V> map(Map<P,V> mapInterfaces) {
        return new ChangeClass<V>(interfaceClasses.map(mapInterfaces), value);
    }

    <V extends PropertyInterface> ChangeClass<V> mapBack(Map<V,P> mapInterfaces) {
        return new ChangeClass<V>(interfaceClasses.mapBack(mapInterfaces), value);
    }

    public String toString() {
        return interfaceClasses.toString() + " - " + value.toString();
    }
}

// по сути Map<InterfaceClass,ClassSet>, а точнее даже UniMap<InterfaceClass,ClassSet>
class ValueClassSet<P extends PropertyInterface> extends SubNodeSet<ChangeClass<P>, ValueClassSet<P>> implements PropertyClass<P> {

    ValueClassSet() {
    }

    ValueClassSet(Set<ChangeClass<P>> iNodes) {
        super(iNodes);
    }

    public ValueClassSet(ClassSet value, InterfaceClassSet<P> interfaceSet) {
        super(new ChangeClass<P>(interfaceSet,value));
    }

    void or(ChangeClass<P> toAdd) {

        for(Iterator<ChangeClass<P>> i = iterator();i.hasNext();) {
            ChangeClass<P> changeClass = i.next();
            // также надо сделать что если совпадают классы то просто за or'ить
            // если мн-во классов включает другое мн-во, то уберем избыточные InterfaceClass
            if(changeClass.value.containsAll(toAdd.value))
                toAdd = new ChangeClass<P>(toAdd.interfaceClasses.excludeAll(changeClass.interfaceClasses),toAdd.value);
            else
            if(toAdd.value.containsAll(changeClass.value))
                changeClass.interfaceClasses.removeAll(toAdd.interfaceClasses);
            else {
                // также надо по хорошему если вижу одинаковые InterfaceClass сделать OR их ClassSet'ов и вырезать их в отдельный ChangeClass
            }
            // уберем старый класс если надо
            if(changeClass.interfaceClasses.isEmpty()) i.remove();
        }
        if(!toAdd.interfaceClasses.isEmpty())
            add(toAdd);
    }

    Set<ChangeClass<P>> and(ChangeClass<P> andNode, ChangeClass<P> node) {
        ClassSet andValue = andNode.value.copy();
        andValue.or(node.value);
        return Collections.singleton(new ChangeClass<P>(andNode.interfaceClasses.and(node.interfaceClasses),andValue));
    }

    ValueClassSet<P> create(Set<ChangeClass<P>> iNodes) {
        return new ValueClassSet<P>(iNodes);
    }

    Map<InterfaceClass<P>,ClassSet> cacheValueClass = new HashMap<InterfaceClass<P>, ClassSet>();
    public ClassSet getValueClass(InterfaceClass<P> interfaceImplement) {
        ClassSet result = null;
        if(Main.activateCaches) result = cacheValueClass.get(interfaceImplement);
        if(result==null) {
            result = new ClassSet();
            for(ChangeClass<P> changeClass : this) // если пересекается хоть с одним, то на выходе может иметь что угодно
                if(!changeClass.interfaceClasses.and(interfaceImplement).isEmpty())
                    result.or(changeClass.value);
            if(Main.activateCaches) cacheValueClass.put(interfaceImplement,result);
        }
        return result;
    }

    Map<ClassSet,InterfaceClassSet<P>> cacheClassSet = new HashMap<ClassSet,InterfaceClassSet<P>>();
    public InterfaceClassSet<P> getClassSet(ClassSet reqValue) {
        InterfaceClassSet<P> result = null;
        if(Main.activateCaches) result = cacheClassSet.get(reqValue);
        if(result==null) {
            result = new InterfaceClassSet<P>();
            for(ChangeClass<P> ChangeClass : this) // по сути надо если пересекается ReqValue
                if(reqValue ==ClassSet.universal || ChangeClass.value.intersect(reqValue))
                    result.or(ChangeClass.interfaceClasses);
            if(Main.activateCaches) cacheClassSet.put(reqValue,result);
        }
        return result;
    }

    public ValueClassSet<P> getValueClassSet() {
        return this;
    }

    <V extends PropertyInterface> ValueClassSet<V> map(Map<P,V> mapInterfaces) {
        ValueClassSet<V> result = new ValueClassSet<V>();
        for(ChangeClass<P> change : this)
            result.add(change.map(mapInterfaces));
        return result;
    }

    <V extends PropertyInterface> ValueClassSet<V> mapBack(Map<V,P> mapInterfaces) {
        ValueClassSet<V> result = new ValueClassSet<V>();
        for(ChangeClass<P> change : this)
            result.add(change.mapBack(mapInterfaces));
        return result;
    }
    
}

class InterfaceClassSet<P extends PropertyInterface> extends GraphNodeSet<InterfaceClass<P>,InterfaceClassSet<P>> {

    InterfaceClassSet() {
    }

    protected InterfaceClassSet(Set<InterfaceClass<P>> iNodes) {
        super(iNodes);
    }

    public InterfaceClassSet(InterfaceClass<P> node) {
        super(Collections.singleton(node));
    }

    boolean has(InterfaceClass<P> orNode, InterfaceClass<P> node) {
        return orNode.hasEmpty() || node.isParent(orNode);
    }

    public boolean isEmpty() {
        return super.isEmpty() || (size()==1 && iterator().next().hasEmpty());
    }

    Set<InterfaceClass<P>> and(InterfaceClass<P> andNode, InterfaceClass<P> node) {
        return Collections.singleton(andNode.and(node));
    }

    InterfaceClassSet<P> create(Set<InterfaceClass<P>> iNodes) {
        return new InterfaceClassSet<P>(iNodes);
    }

    public Map<P,Class> getCommonParent() {
        Map<P,Class> result = new HashMap<P,Class>();
        for(P propertyInterface : iterator().next().keySet()) {
            ClassSet commonClassSet = new ClassSet();
            for(InterfaceClass<P> node : this)
                commonClassSet.or(node.get(propertyInterface));
            result.put(propertyInterface,commonClassSet.getCommonClass());
        }
        return result;
    }

    <V extends PropertyInterface> InterfaceClassSet<V> map(Map<P,V> mapInterfaces) {
        InterfaceClassSet<V> result = new InterfaceClassSet<V>();
        for(InterfaceClass<P> node : this)
            result.add(node.map(mapInterfaces));
        return result;
    }

    <V extends PropertyInterface> InterfaceClassSet<V> mapBack(Map<V,P> mapInterfaces) {
        InterfaceClassSet<V> result = new InterfaceClassSet<V>();
        for(InterfaceClass<P> node : this)
            result.add(node.mapBack(mapInterfaces));
        return result;
    }
}
