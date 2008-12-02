package platformlocal;

import java.util.*;

abstract class SubNodeSet<T,S extends SubNodeSet<T,S>> extends HashSet<T> {

    protected SubNodeSet() {
    }

    protected SubNodeSet(Set<T> iNodes) {
        super(iNodes);
    }

    protected SubNodeSet(T Node) {
        add(Node);
    }

    void or(S Set) {
        for(T Node : Set)
            or(Node);
    }
    abstract void or(T OrNode);

    S and(S Set) {
        S AndSet = create(new HashSet<T>());
        for(T Node : this)
            AndSet.or(Set.and(Node));
        return AndSet;
    }
    S and(T AndNode) {
        S AndSet = create(new HashSet<T>());
        for(T Node : this) AndSet.or(create(and(AndNode,Node)));
        return AndSet;
    }
    abstract Set<T> and(T AndNode,T Node);
    abstract S create(Set<T> iNodes);

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubNodeSet that = (SubNodeSet) o;

        return super.equals(that);
    }

    public int hashCode() {
        return super.hashCode();
    }
}

abstract class GraphNodeSet<T,S extends GraphNodeSet<T,S>> extends SubNodeSet<T,S> {

    protected GraphNodeSet() {
    }

    protected GraphNodeSet(Set<T> iNodes) {
        super(iNodes);
    }

    boolean has(T CheckNode) {
        for(T Node : this)
            if(has(CheckNode,Node)) return true;
        return false;
    }
    abstract boolean has(T OrNode,T Node);

    void or(T OrNode) {
        for(T Node : this)
            if(has(OrNode,Node)) return;
        for(Iterator<T> i=iterator();i.hasNext();)
            if(has(i.next(),OrNode)) i.remove();
        add(OrNode);
    }

    void removeAll(GraphNodeSet<T,S> ToRemove) {
        for(Iterator<T> i = iterator();i.hasNext();)
            if(ToRemove.has(i.next())) i.remove();
    }

    S excludeAll(GraphNodeSet<T,S> ToRemove) {
        S Result = create(new HashSet<T>());
        for(T Node : this)
            if(!ToRemove.has(Node)) Result.add(Node);
        return Result;
    }

}

// выше вершин
class UpClassSet extends GraphNodeSet<Class,UpClassSet> {

    UpClassSet() {
    }

    protected UpClassSet(Set<Class> iNodes) {
        super(iNodes);
    }

    boolean has(Class OrNode, Class Node) {
        return OrNode.isParent(Node);
    }

    Set<Class> and(Class AndNode, Class Node) {
        return AndNode.commonChilds(Node);
    }

    UpClassSet create(Set<Class> iNodes) {
        return new UpClassSet(iNodes);
    }

    Set<Class> andSet(Set<Class> Set) {
        Set<Class> Result = new HashSet<Class>();
        for(Class Node : Set)
            if(has(Node)) Result.add(Node);
        return Result;
    }

    void removeSet(Set<Class> Set) {
        for(Iterator<Class> i = Set.iterator();i.hasNext();)
            if(has(i.next())) i.remove();
    }
}

// по сути на Or
class ClassSet {
    UpClassSet Up;
    Set<Class> Set;

    ClassSet(UpClassSet iUp, Set<Class> iSet) {
        Up = iUp;
        Set = iSet;
    }

    ClassSet() {
        Up = new UpClassSet();
        Set = new HashSet<Class>();
    }

    ClassSet(Class Node) {
        this(new UpClassSet(),Collections.singleton(Node));
    }

    static ClassSet getUp(Class Node) {
        return new ClassSet(new UpClassSet(Collections.singleton(Node)),new HashSet<Class>());
    }

    ClassSet and(ClassSet Node) {
        // Up*Node.Up OR (Up*Node.Set+Set*Node.Up+Set*Node.Set) (легко доказать что второе не пересек. с первым)
        Set<Class> AndSet = CollectionExtend.intersect(Set,Node.Set);
        AndSet.addAll(Up.andSet(Node.Set));
        AndSet.addAll(Node.Up.andSet(Set));
        return new ClassSet(Up.and(Node.Up),AndSet);
    }

    void or(ClassSet Node) {
        // or'им Up'ы, or'им Set'ы после чего вырезаем из Set'а все кто есть в Up'ах
        Up.or(Node.Up);
        Set.addAll(Node.Set);
        for(Iterator<Class> i = Set.iterator();i.hasNext();)
            if(Up.has(i.next())) i.remove();                
    }

    // входит ли в дерево элемент
    boolean contains(Class Node) {
        return Set.contains(Node) || Up.has(Node);
    }
    boolean isEmpty() {
        return Set.isEmpty() && Up.isEmpty(); 
    }
    
    boolean containsAll(ClassSet Node) {
        return and(Node).equals(Node);            
    }
    boolean intersect(ClassSet Node) {
        return !and(Node).isEmpty();
    }
    Type getType() {
        return getCommonClass().getType();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassSet classSet = (ClassSet) o;

        return Set.equals(classSet.Set) && Up.equals(classSet.Up);  
    }

    public int hashCode() {
        int result;
        result = Up.hashCode();
        result = 31 * result + Set.hashCode();
        return result;
    }

    Class getCommonClass() {
        if(isEmpty())
            throw new RuntimeException("Empty Base Class");

        Set<Class> CommonParents = new HashSet<Class>(Set);
        CommonParents.addAll(Up);
        while(CommonParents.size()>1) {
            Iterator<Class> i = CommonParents.iterator();
            Class First = i.next(); i.remove();
            Class Second = i.next(); i.remove();
            CommonParents.addAll(First.commonParents(Second));
        }
        return CommonParents.iterator().next();
    }

    Class getRandom(Random Randomizer) {
        // пока чисто по Up'у
        return CollectionExtend.getRandom(Up,Randomizer);
    }

    static ClassSet universal;
    static {
        universal = getUp(Class.base);
    }

    public String toString() {
        return (!Set.isEmpty()?Set.toString():"")+(!Up.isEmpty() && !Set.isEmpty()?" ":"")+(!Up.isEmpty()?"Up:"+Up.toString():"");
    }

    public ClassSet copy() {
        return new ClassSet(new UpClassSet(Up),new HashSet<Class>(Set));
    }
}

//>;

class InterfaceClass<P extends PropertyInterface> extends HashMap<P,ClassSet> {

    InterfaceClass(Map<P, ObjectValue> Keys) {
        for(Map.Entry<P,ObjectValue> Key : Keys.entrySet())
            put(Key.getKey(),new ClassSet(Key.getValue().Class));
    }

    InterfaceClass() {
    }

    InterfaceClass(P Interface, ClassSet InterfaceValue) {
        put(Interface,InterfaceValue);
    }

    // когда каждый ClassSet включает в себя все подмн-ва
    public boolean isParent(InterfaceClass<P> Node) {
        // здесь строго говоря доджен keySet'ы должны совпадать
        // DEBUG проверка
        if(!keySet().equals(Node.keySet()))
            throw new RuntimeException("different interfaces");
        for(Map.Entry<P,ClassSet> Interface : entrySet())
            if(!Interface.getValue().containsAll(Node.get(Interface.getKey()))) return false;
        return true;
    }

    public InterfaceClass<P> and(InterfaceClass<P> Node) {
        InterfaceClass<P> And = new InterfaceClass<P>();
        And.putAll(Node);
        for(Map.Entry<P,ClassSet> Interface : entrySet()) {
            ClassSet AndClass = And.get(Interface.getKey());
            if(AndClass==null)
                AndClass = Interface.getValue();
            else
                AndClass = AndClass.and(Interface.getValue());
            And.put(Interface.getKey(),AndClass);
        }
        return And;
    }

    <V extends PropertyInterface> InterfaceClass<V> map(Map<P,V> MapInterfaces) {
        InterfaceClass<V> Result = new InterfaceClass<V>();
        for(Map.Entry<P,V> Interface : MapInterfaces.entrySet())
            Result.put(Interface.getValue(),get(Interface.getKey()));
        return Result;
    }

    <V extends PropertyInterface> InterfaceClass<V> mapBack(Map<V,P> MapInterfaces) {
        InterfaceClass<V> Result = new InterfaceClass<V>();
        for(Map.Entry<V,P> Interface : MapInterfaces.entrySet())
            Result.put(Interface.getKey(),get(Interface.getValue()));
        return Result;
    }

    public boolean hasEmpty() {
        for(ClassSet Set : values())
            if(Set.isEmpty()) return true;
        return false;
    }
}

// по сути это Map<InterfaceClass,ClassSet> с определенными ограничениями
// точнее нужна такая структура при которой при одинаковых ClassSet она будет Or'ить InterfaceClassSet'ы - собсно и нужен на самом деле UniMap<InterfaceClassSet,ClassSet>

interface PropertyClass<P extends PropertyInterface> {
    // каких классов могут быть объекты при таких классах на входе !!!! Можно дать больше но никак не меньше
    ClassSet getValueClass(InterfaceClass<P> InterfaceImplement);
    // при каких классах мы можем получить хоть один такой класс (при других точно не можем) !!! опять таки лучше больше чем меньше
    InterfaceClassSet<P> getClassSet(ClassSet ReqValue);

    // на самом деле нам нужен iterator по <InterfaceClass,ClassSet>
    ValueClassSet<P> getValueClassSet();
}

// по сути Entry для ValueClassSet'а
class ChangeClass<P extends PropertyInterface> {

    InterfaceClassSet<P> Interface;
    ClassSet Value;

    ChangeClass() {
        Interface = new InterfaceClassSet<P>();
        Value = new ClassSet();
    }

    ChangeClass(InterfaceClassSet<P> iInterface, ClassSet iValue) {
        Interface = iInterface;
        Value = iValue;
    }

    ChangeClass(InterfaceClass<P> iInterface, ClassSet iValue) {
        Interface = new InterfaceClassSet<P>(iInterface);
        Value = iValue;
    }

    public ChangeClass(P iInterface, ClassSet iInterfaceValue) {
        Interface = new InterfaceClassSet<P>(new InterfaceClass<P>(iInterface,iInterfaceValue));
        Value = new ClassSet();
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

      public ChangeClass(Class ValueClass) {
          Value = new OrClassSet(ValueClass);
          Interface = new InterfaceClassSet<P>();
      }
    */
    <V extends PropertyInterface> ChangeClass<V> map(Map<P,V> MapInterfaces) {
        return new ChangeClass<V>(Interface.map(MapInterfaces),Value);
    }

    <V extends PropertyInterface> ChangeClass<V> mapBack(Map<V,P> MapInterfaces) {
        return new ChangeClass<V>(Interface.mapBack(MapInterfaces),Value);
    }

    public String toString() {
        return Interface.toString() + " - " + Value.toString();
    }
}

// по сути Map<InterfaceClass,ClassSet>, а точнее даже UniMap<InterfaceClass,ClassSet>
class ValueClassSet<P extends PropertyInterface> extends SubNodeSet<ChangeClass<P>, ValueClassSet<P>> implements PropertyClass<P> {

    ValueClassSet() {
    }

    ValueClassSet(Set<ChangeClass<P>> iNodes) {
        super(iNodes);
    }

    public ValueClassSet(ClassSet Value, InterfaceClassSet<P> Interface) {
        super(new ChangeClass<P>(Interface,Value));
    }

    void or(ChangeClass<P> ToAdd) {

        for(Iterator<ChangeClass<P>> i = iterator();i.hasNext();) {
            ChangeClass<P> Class = i.next();
            // также надо сделать что если совпадают классы то просто за or'ить
            // если мн-во классов включает другое мн-во, то уберем избыточные InterfaceClass
            if(Class.Value.containsAll(ToAdd.Value))
                ToAdd = new ChangeClass<P>(ToAdd.Interface.excludeAll(Class.Interface),ToAdd.Value);
            else
            if(ToAdd.Value.containsAll(Class.Value))
                Class.Interface.removeAll(ToAdd.Interface);
            else {
                // также надо по хорошему если вижу одинаковые InterfaceClass сделать OR их ClassSet'ов и вырезать их в отдельный ChangeClass
            }
            // уберем старый класс если надо
            if(Class.Interface.isEmpty()) i.remove();
        }
        if(!ToAdd.Interface.isEmpty())
            add(ToAdd);
    }

    Set<ChangeClass<P>> and(ChangeClass<P> AndNode, ChangeClass<P> Node) {
        ClassSet AndValue = AndNode.Value.copy();
        AndValue.or(Node.Value);
        return Collections.singleton(new ChangeClass<P>(AndNode.Interface.and(Node.Interface),AndValue));
    }

    ValueClassSet<P> create(Set<ChangeClass<P>> iNodes) {
        return new ValueClassSet<P>(iNodes);
    }

    Map<InterfaceClass<P>,ClassSet> CacheValueClass = new HashMap<InterfaceClass<P>, ClassSet>();
    public ClassSet getValueClass(InterfaceClass<P> InterfaceImplement) {
        ClassSet Result = null;
        if(Main.ActivateCaches) Result = CacheValueClass.get(InterfaceImplement);
        if(Result==null) {
            Result = new ClassSet();
            for(ChangeClass<P> Class : this) // если пересекается хоть с одним, то на выходе может иметь что угодно
                if(!Class.Interface.and(InterfaceImplement).isEmpty())
                    Result.or(Class.Value);
            if(Main.ActivateCaches) CacheValueClass.put(InterfaceImplement,Result);
        }
        return Result;
    }

    Map<ClassSet,InterfaceClassSet<P>> CacheClassSet = new HashMap<ClassSet,InterfaceClassSet<P>>();
    public InterfaceClassSet<P> getClassSet(ClassSet ReqValue) {
        InterfaceClassSet<P> Result = null;
        if(Main.ActivateCaches) Result = CacheClassSet.get(ReqValue);
        if(Result==null) {
            Result = new InterfaceClassSet<P>();
            for(ChangeClass<P> ChangeClass : this) // по сути надо если пересекается ReqValue
                if(ReqValue==ClassSet.universal || ChangeClass.Value.intersect(ReqValue))
                    Result.or(ChangeClass.Interface);
            if(Main.ActivateCaches) CacheClassSet.put(ReqValue,Result);
        }
        return Result;
    }

    public ValueClassSet<P> getValueClassSet() {
        return this;
    }

    <V extends PropertyInterface> ValueClassSet<V> map(Map<P,V> MapInterfaces) {
        ValueClassSet<V> Result = new ValueClassSet<V>();
        for(ChangeClass<P> Change : this)
            Result.add(Change.map(MapInterfaces));
        return Result;
    }

    <V extends PropertyInterface> ValueClassSet<V> mapBack(Map<V,P> MapInterfaces) {
        ValueClassSet<V> Result = new ValueClassSet<V>();
        for(ChangeClass<P> Change : this)
            Result.add(Change.mapBack(MapInterfaces));
        return Result;
    }
    
}

class InterfaceClassSet<P extends PropertyInterface> extends GraphNodeSet<InterfaceClass<P>,InterfaceClassSet<P>> {

    InterfaceClassSet() {
    }

    protected InterfaceClassSet(Set<InterfaceClass<P>> iNodes) {
        super(iNodes);
    }

    public InterfaceClassSet(InterfaceClass<P> Node) {
        super(Collections.singleton(Node));
    }

    boolean has(InterfaceClass<P> OrNode, InterfaceClass<P> Node) {
        return OrNode.hasEmpty() || Node.isParent(OrNode);
    }

    public boolean isEmpty() {
        return super.isEmpty() || (size()==1 && iterator().next().hasEmpty());
    }

    Set<InterfaceClass<P>> and(InterfaceClass<P> AndNode, InterfaceClass<P> Node) {
        return Collections.singleton(AndNode.and(Node));
    }

    InterfaceClassSet<P> create(Set<InterfaceClass<P>> iNodes) {
        return new InterfaceClassSet<P>(iNodes);
    }

    public Map<P,Class> getCommonParent() {
        Map<P,Class> Result = new HashMap<P,Class>();
        for(P Interface : iterator().next().keySet()) {
            ClassSet CommonClassSet = new ClassSet();
            for(InterfaceClass<P> Node : this)
                CommonClassSet.or(Node.get(Interface));
            Result.put(Interface,CommonClassSet.getCommonClass());
        }
        return Result;
    }

    <V extends PropertyInterface> InterfaceClassSet<V> map(Map<P,V> MapInterfaces) {
        InterfaceClassSet<V> Result = new InterfaceClassSet<V>();
        for(InterfaceClass<P> Node : this)
            Result.add(Node.map(MapInterfaces));
        return Result;
    }

    <V extends PropertyInterface> InterfaceClassSet<V> mapBack(Map<V,P> MapInterfaces) {
        InterfaceClassSet<V> Result = new InterfaceClassSet<V>();
        for(InterfaceClass<P> Node : this)
            Result.add(Node.mapBack(MapInterfaces));
        return Result;
    }
}


/*class ClassInterfaceSet extends ArrayList<ClassInterface> {

    ClassInterface GetCommonParent() {
        Iterator<ClassInterface> i = iterator();
        ClassInterface Result = i.next();
        while(i.hasNext()) Result.CommonParent(i.next());
        return Result;
    }

    void Out(Collection<PropertyInterface> ToDraw) {
        for(ClassInterface InClass : this) {
            for(PropertyInterface Key : ToDraw)
                System.out.print(InClass.get(Key).ID.toString()+" ");
            System.out.println();
       }
   }

    // нужен интерфейс слияния и пересечения с ClassInterface

    ClassInterfaceSet AndSet(ClassInterfaceSet Op) {
//        if(size()==0) return (ClassInterfaceSet)Op.clone();
//        if(Op.size()==0) return (ClassInterfaceSet)clone();
        ClassInterfaceSet Result = new ClassInterfaceSet();
        for(ClassInterface IntClass : this)
            Result.OrSet(Op.AndItem(IntClass));
        return Result;
    }

    void OrSet(ClassInterfaceSet Op) {
        for(ClassInterface IntClass : Op) OrItem(IntClass);
    }

    ClassInterfaceSet AndItem(ClassInterface Op) {
        ClassInterfaceSet Result = new ClassInterfaceSet();
//        if(size()>0) {
            for(ClassInterface IntClass : this) Result.OrSet(Op.And(IntClass));
//        } else
//            Result.add(Op);

        return Result;
    }

    boolean OrItem(ClassInterface Op) {
        // бежим по всем, если выше какого-то класса, если ниже, то старый выкидываем
        Iterator<ClassInterface> i = iterator();
        while(i.hasNext()) {
            ClassInterface OrInterface = i.next();
            int OrResult = OrInterface.Or(Op);
            if(OrResult==1) return true;
            if(OrResult==2) i.remove();
        }

        add(Op);

        return false;
    }

    @Override public Object clone() {
        ClassInterfaceSet CloneObject = new ClassInterfaceSet();
        for(ClassInterface IntClass : this) CloneObject.add(IntClass);
        return CloneObject;
    }
}


class ClassInterface extends HashMap<PropertyInterface,Class> {

    @Override
    public Class put(PropertyInterface key, Class value) {
        if(value==null)
            throw new RuntimeException();
        return super.put(key, value);
    }

    ClassInterfaceSet And(ClassInterface AndOp) {
        ClassInterfaceSet Result = new ClassInterfaceSet();

        Map<Class[],PropertyInterface> JoinClasses = new HashMap<Class[],PropertyInterface>();

        Class Class;
        Class[] SingleArray;

        for(PropertyInterface Key : keySet()) {
            Class = get(Key);
            Class AndClass = AndOp.get(Key);

            if(AndClass!=null) {
                Class[] CommonClasses = (Class[])Class.CommonClassSet(AndClass).toArray(new Class[0]);
                // если не нашли ни одного общего класса, то выходим
                if(CommonClasses.length==0) return Result;
                JoinClasses.put(CommonClasses,Key);
            }
            else {
                SingleArray = new Class[1];
                SingleArray[0] = Class;
                JoinClasses.put(SingleArray,Key);
            }
        }

        for(PropertyInterface Key : AndOp.keySet()) {
            if(!containsKey(Key)) {
                SingleArray = new Class[1];
                SingleArray[0] = AndOp.get(Key);
                JoinClasses.put(SingleArray,Key);
            }
        }

        int ia;
        Class[][] ArrayClasses = (Class[][])JoinClasses.keySet().toArray(new Class[0][]);
        PropertyInterface[] ArrayInterfaces = new PropertyInterface[ArrayClasses.length];
        int[] IntIterators = new int[ArrayClasses.length];
        for(ia=0;ia<ArrayClasses.length;ia++) {
            ArrayInterfaces[ia] = JoinClasses.get(ArrayClasses[ia]);
            IntIterators[ia] = 0;
        }
        boolean Exit = false;
        while(!Exit) {
            // закидываем новые комбинации
            ClassInterface ResultInterface = new ClassInterface();
            for(ia=0;ia<ArrayClasses.length;ia++) ResultInterface.put(ArrayInterfaces[ia],ArrayClasses[ia][IntIterators[ia]]);
            Result.add(ResultInterface);

            // следующую итерацию
            while(ia<ArrayClasses.length && IntIterators[ia]==ArrayClasses[ia].length-1) {
                IntIterators[ia] = 0;
                ia++;
            }

            if(ia>=ArrayClasses.length) Exit=true;
        }

        return Result;
    }

    // 0 - не связаны, 1 - Op >= , 2 - Op <
    // известно что одной размерности
    int Or(ClassInterface OrOp) {

        int ResultOr = -1;
        for(PropertyInterface Key : keySet()) {
            Class Class = get(Key);
            Class OrClass = OrOp.get(Key);

            if(Class!=OrClass) {
                // отличающийся
                if(ResultOr<2) {
                    if(OrClass.IsParent(Class))
                        ResultOr = 1;
                    else
                        if(ResultOr==1)
                            return 0;
                }

                if(ResultOr!=1)
                    if(Class.IsParent(OrClass))
                        ResultOr = 2;
                    else
                        return 0;
                }
            }

        if(ResultOr==-1) return 1;
        return ResultOr;
    }

    // известно что одной размерности
    void CommonParent(ClassInterface Op) {
        for(PropertyInterface Key : keySet())
            put(Key,get(Key).CommonParent(Op.get(Key)));
    }

    public boolean isRequired(InterfaceClassSet<?> InterfaceClasses) {
        for(Map.Entry<PropertyInterface,Class> MapInterface : entrySet()) {
            ClassSet ClassSet = InterfaceClasses.get(MapInterface.getKey());
            if(ClassSet!=null)
                for(Class ReqClass : ClassSet)
                    if(MapInterface.getValue().IsParent(ReqClass))
                        return true;
        }
        return false;
    }
}

class ClassSet extends HashSet<Class> {

    public ClassSet(Class iClass) {
        add(iClass);
    }

    public ClassSet() {
    }

    public ClassSet(Collection<Class> Classes) {
        for(Class AddClass : Classes) and(AddClass);
    }

    void and(ClassSet Op) {
        // возвращает все классы операндов
        for(Class OpClass : Op)
            and(OpClass);
    }

    void and(Class AndClass) {
        // если не parent ни одного, добавляем удаляя те кто isParent
        for(Class Class : this)
            if(AndClass.IsParent(Class)) return;
        for(Iterator<Class> i=iterator();i.hasNext();)
            if(i.next().IsParent(AndClass)) i.remove();
        add(AndClass);
    }

    ClassSet contains(ClassSet Op) {
        // возвращает конкретные классы
        ClassSet Result = new ClassSet();
        for(Class Class : Op)
            Result.and(contains(Class));
        return Result;
    }

    ClassSet contains(Class OrClass) {
        ClassSet Result = new ClassSet();
        for(Class Class : this)
            Result.and(Class.CommonClassSet(OrClass));
        return Result;
    }
}

class InterfaceClassSet<T extends PropertyInterface> extends HashMap<T, ClassSet> {

    public ClassSet put(T key, ClassSet value) {
        if(value==null)
            throw new RuntimeException();
        return super.put(key, value);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public InterfaceClassSet() {
    }

    public InterfaceClassSet(T Interface, Class Class) {
        put(Interface,new ClassSet(Class));
    }

    <V extends PropertyInterface> InterfaceClassSet<V> mapChange(Map<T,V> MapInterfaces) {
        InterfaceClassSet<V> Result = new InterfaceClassSet<V>();
        for(Map.Entry<T,V> Interface : MapInterfaces.entrySet()) {
            ClassSet MapChange = get(Interface.getKey());
            if(MapChange!=null) Result.put(Interface.getValue(),MapChange);
        }
        return Result;
    }

    <V extends PropertyInterface> InterfaceClassSet<V> mapBackChange(Map<V,T> MapInterfaces) {
        InterfaceClassSet<V> Result = new InterfaceClassSet<V>();
        for(Map.Entry<V,T> Interface : MapInterfaces.entrySet()) {
            ClassSet MapChange = get(Interface.getValue());
            if(MapChange!=null) Result.put(Interface.getKey(),MapChange);
        }
        return Result;
    }

    void contains(InterfaceClassSet<T> ToAdd) {
        for(Iterator<Map.Entry<T,ClassSet>> i=entrySet().iterator();i.hasNext();) {
            Map.Entry<T,ClassSet> MapInterface = i.next();
            ClassSet AddSet = ToAdd.get(MapInterface.getKey());
            ClassSet OrSet = (AddSet!=null ? MapInterface.getValue().contains(AddSet) : new ClassSet());
            if(OrSet.size()>0)
                MapInterface.setValue(OrSet);
            else
                i.remove();
        }
    }

    void and(InterfaceClassSet<T> ToAdd) {
        for(Map.Entry<T,ClassSet> MapInterface : ToAdd.entrySet()) {
            ClassSet ClassSet = get(MapInterface.getKey());
            if(ClassSet!=null)
                ClassSet.and(MapInterface.getValue());
            else
                put(MapInterface.getKey(),MapInterface.getValue());
        }
    }
}
  */