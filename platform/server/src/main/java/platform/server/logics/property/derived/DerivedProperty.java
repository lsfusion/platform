package platform.server.logics.property.derived;

import platform.interop.Compare;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.classes.ConcreteValueClass;
import platform.server.classes.ValueClass;
import platform.server.classes.DoubleClass;
import platform.server.logics.property.*;

import java.util.*;

public class DerivedProperty {

    private static int ids = 0;
    public static String genID() {
        return "GDVID"+(ids++);
    }

    private static ConcreteValueClass formulaClass = DoubleClass.instance; 

    // создает "раздваивающее" свойство (self join в SQL)
    private static <T extends PropertyInterface,P extends PropertyInterface> JoinProperty<P> createSelfProp(Collection<T> interfaces, Map<T,JoinProperty.Interface> map1, Map<T,JoinProperty.Interface> map2, Set<T> partInterfaces) {
        JoinProperty<P> joinProperty = new JoinProperty<P>(genID(),"sys",interfaces.size()*2-partInterfaces.size(),false);
        Iterator<JoinProperty.Interface> joinIterator = joinProperty.interfaces.iterator();
        for(T propertyInterface : interfaces) {
            JoinProperty.Interface joinInterface = joinIterator.next();
            map1.put(propertyInterface, joinInterface);
            map2.put(propertyInterface, (partInterfaces.contains(propertyInterface)?joinInterface:joinIterator.next()));
        }
        return joinProperty;
    }

    private static void compareJoin(JoinProperty<CompareFormulaProperty.Interface> joinProperty, Compare compare, PropertyInterfaceImplement<JoinProperty.Interface> operator1, PropertyInterfaceImplement<JoinProperty.Interface> operator2) {
        CompareFormulaProperty compareProperty = new CompareFormulaProperty(genID(),compare);

        Map<CompareFormulaProperty.Interface,PropertyInterfaceImplement<JoinProperty.Interface>> mapImplement = new HashMap<CompareFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>();
        mapImplement.put(compareProperty.operator1, operator1);
        mapImplement.put(compareProperty.operator2, operator2);

        joinProperty.implement = new PropertyImplement<PropertyInterfaceImplement<JoinProperty.Interface>,CompareFormulaProperty.Interface>(compareProperty,mapImplement);
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> PropertyMapImplement<?,JoinProperty.Interface> createCompareProp(PropertyMapImplement<P,T> implement, Map<T,JoinProperty.Interface> map1, Map<T,JoinProperty.Interface> map2, Compare compare) {
        Map<P,JoinProperty.Interface> join1 = new HashMap<P, JoinProperty.Interface>(); Map<P,JoinProperty.Interface> join2 = new HashMap<P, JoinProperty.Interface>();
        JoinProperty<CompareFormulaProperty.Interface> joinProperty = createSelfProp(implement.property.interfaces, join1, join2, new HashSet<P>());
        compareJoin(joinProperty, compare, new PropertyMapImplement<P,JoinProperty.Interface>(implement.property,join1), new PropertyMapImplement<P,JoinProperty.Interface>(implement.property,join2));
        return new PropertyMapImplement<JoinProperty.Interface,JoinProperty.Interface>(joinProperty, BaseUtils.merge(
                BaseUtils.crossJoin(join1,BaseUtils.join(implement.mapping,map1)),
                BaseUtils.crossJoin(join2,BaseUtils.join(implement.mapping,map2))));
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> PropertyMapImplement<?,JoinProperty.Interface> createInterfaceCompareMap(T implement, Map<T,JoinProperty.Interface> map1, Map<T,JoinProperty.Interface> map2, Compare compare) {
        CompareFormulaProperty compareProperty = new CompareFormulaProperty(genID(),compare);

        Map<CompareFormulaProperty.Interface,JoinProperty.Interface> mapImplement = new HashMap<CompareFormulaProperty.Interface, JoinProperty.Interface>();
        mapImplement.put(compareProperty.operator1, map1.get(implement));
        mapImplement.put(compareProperty.operator2, map2.get(implement));

        return new PropertyMapImplement<CompareFormulaProperty.Interface,JoinProperty.Interface>(compareProperty,mapImplement);
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> PropertyMapImplement<?,JoinProperty.Interface> createCompareMap(PropertyInterfaceImplement<T> implement, Map<T,JoinProperty.Interface> map1, Map<T,JoinProperty.Interface> map2, Compare compare) {
        if(implement instanceof PropertyMapImplement)
            return createCompareProp((PropertyMapImplement<?,T>)implement, map1, map2, compare);
        else
            return createInterfaceCompareMap((T)implement, map1, map2, compare);
    }

    public static <T extends PropertyInterface> JoinProperty<AndFormulaProperty.Interface> createPartition(Collection<T> interfaces, PropertyInterfaceImplement<T> property, Collection<PropertyInterfaceImplement<T>> partitions, PropertyInterfaceImplement<T> expr, Map<T, JoinProperty.Interface> mapMain, Compare compare) {
        // "двоим" интерфейсы (для partition'а не PropertyInterface), для результ. св-ва
        Set<T> partInterfaces = new HashSet<T>();
        Set<PropertyMapImplement<?,T>> partProperties = new HashSet<PropertyMapImplement<?,T>>();
        for(PropertyInterfaceImplement<T> partition : partitions)
            partition.fill(partInterfaces, partProperties);

        Map<T,JoinProperty.Interface> mapDupl = new HashMap<T, JoinProperty.Interface>();
        JoinProperty<AndFormulaProperty.Interface> joinProperty = createSelfProp(interfaces, mapMain,mapDupl,partInterfaces);

        // ставим equals'ы на partitions свойства (раздвоенные), greater на предшествие order (раздвоенное)
        AndFormulaProperty andPrevious = new AndFormulaProperty(genID(),new boolean[partProperties.size()+1]);
        Map<AndFormulaProperty.Interface,PropertyInterfaceImplement<JoinProperty.Interface>> mapImplement = new HashMap<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>();
        mapImplement.put(andPrevious.objectInterface,property.map(mapDupl));
        Iterator<AndFormulaProperty.AndInterface> itAnd = andPrevious.andInterfaces.iterator();
        for(PropertyMapImplement<?, T> partProperty : partProperties)
            mapImplement.put(itAnd.next(), createCompareProp(partProperty, mapMain, mapDupl, Compare.EQUALS));
        mapImplement.put(itAnd.next(),createCompareMap(expr, mapMain, mapDupl, compare));

        joinProperty.implement = new PropertyImplement<PropertyInterfaceImplement<JoinProperty.Interface>,AndFormulaProperty.Interface>(andPrevious, mapImplement);
        return joinProperty;
    }
    
    protected static <T extends PropertyInterface> PropertyMapImplement<?,T> createSOProp(Property<T> property, Collection<PropertyInterfaceImplement<T>> partitions, PropertyInterfaceImplement<T> order, boolean ascending, boolean last) {

        Map<T,JoinProperty.Interface> mapMain = new HashMap<T, JoinProperty.Interface>();
        JoinProperty<AndFormulaProperty.Interface> joinProperty = createPartition(property.interfaces, property.getImplement(), partitions, order, mapMain,
                ascending ? (last ? Compare.LESS_EQUALS : Compare.LESS) : (last ? Compare.GREATER_EQUALS : Compare.GREATER));

        // создаем groupProperty для map2 и даем их на выход
        SumGroupProperty<JoinProperty.Interface> groupProperty = new SumGroupProperty<JoinProperty.Interface>(genID(),"sys",mapMain.values(),joinProperty);
        Map<GroupProperty.Interface<JoinProperty.Interface>,JoinProperty.Interface> mapInterfaces = BaseUtils.immutableCast(groupProperty.getMapInterfaces());
        return new PropertyMapImplement<GroupProperty.Interface<JoinProperty.Interface>,T>(groupProperty,BaseUtils.join(mapInterfaces,BaseUtils.reverse(mapMain)));
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createOProp(Property<T> property, Collection<PropertyInterfaceImplement<T>> partitions, OrderedMap<PropertyInterfaceImplement<T>,Boolean> orders, boolean includeLast) {
        assert orders.size()>0;

        if(false) {
            OrderProperty<T> orderProperty = new OrderProperty<T>(genID(), "sys", property, partitions, orders, includeLast);
            return new PropertyMapImplement<OrderProperty.Interface<T>,T>(orderProperty,orderProperty.getMapInterfaces());
        }


        if(orders.size()==1) return createSOProp(property, partitions, orders.singleKey(), orders.singleValue(), includeLast);
        // итеративно делаем Union, перекидывая order'ы в partition'ы
        SumUnionProperty unionProperty = new SumUnionProperty(genID(),"sys",property.interfaces.size());
        Map<T,UnionProperty.Interface> mapInterfaces = BaseUtils.buildMap(property.interfaces,unionProperty.interfaces);

        Iterator<Map.Entry<PropertyInterfaceImplement<T>,Boolean>> it = orders.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<PropertyInterfaceImplement<T>,Boolean> order = it.next();
            unionProperty.operands.put(createSOProp(property, partitions, order.getKey(), order.getValue(), it.hasNext() && includeLast).map(mapInterfaces),1);
            partitions = new ArrayList<PropertyInterfaceImplement<T>>(partitions);
            partitions.add(order.getKey());
        }

        return new PropertyMapImplement<UnionProperty.Interface,T>(unionProperty,BaseUtils.reverse(mapInterfaces));
    }

    // строится partition с пустым order'ом    
    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createPProp(Property<T> property, Collection<PropertyInterfaceImplement<T>> partitions) {
        SumGroupProperty<T> partitionSum = new SumGroupProperty<T>(genID(),"sys",partitions,property);
        
        JoinProperty<GroupProperty.Interface<T>> joinProperty = new JoinProperty<GroupProperty.Interface<T>>(genID(),"sys",property.interfaces.size(),false);
        Map<T,JoinProperty.Interface> joinMap = BaseUtils.buildMap(property.interfaces,joinProperty.interfaces);

        joinProperty.implement = new PropertyImplement<PropertyInterfaceImplement<JoinProperty.Interface>,GroupProperty.Interface<T>>(partitionSum,mapImplements(partitionSum.getMapInterfaces(),joinMap));

        return new PropertyMapImplement<JoinProperty.Interface,T>(joinProperty, BaseUtils.reverse(joinMap));
    }

    // строит процент в partition'е
    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createPOProp(Property<T> property, Collection<PropertyInterfaceImplement<T>> partitions, OrderedMap<PropertyInterfaceImplement<T>,Boolean> orders, boolean includeLast) {
        PropertyMapImplement<?,T> orderSum = createOProp(property, partitions, orders, includeLast);

        PropertyMapImplement<?,T> partitionSum = createPProp(property, partitions);

        // создаем процент
        JoinProperty<StringFormulaProperty.Interface> joinProperty = new JoinProperty<StringFormulaProperty.Interface>(genID(),"sys",property.interfaces.size(),false);
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(property.interfaces, joinProperty.interfaces);

        StringFormulaProperty implement = new StringFormulaProperty(genID(),formulaClass,"(prm1*100)/prm2",2);
        Map<StringFormulaProperty.Interface,PropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = new HashMap<StringFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>();
        joinImplement.put(implement.findInterface("prm1"),orderSum.map(joinMap));
        joinImplement.put(implement.findInterface("prm2"),partitionSum.map(joinMap));

        joinProperty.implement = new PropertyImplement<PropertyInterfaceImplement<JoinProperty.Interface>,StringFormulaProperty.Interface>(implement,joinImplement);
        return new PropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> Map<L,PropertyInterfaceImplement<K>> mapImplements(Map<L,PropertyInterfaceImplement<T>> interfaceImplements, Map<T,K> map) {
        Map<L,PropertyInterfaceImplement<K>> mapImplement = new HashMap<L, PropertyInterfaceImplement<K>>();
        for(Map.Entry<L,PropertyInterfaceImplement<T>> interfaceImplementEntry : interfaceImplements.entrySet())
            mapImplement.put(interfaceImplementEntry.getKey(),interfaceImplementEntry.getValue().map(map));
        return mapImplement;
    }

    private static <L extends PropertyInterface, T extends PropertyInterface> PropertyMapImplement<?,T> createJoinProp(PropertyImplement<PropertyInterfaceImplement<T>,L> implement) {
        // определяем какие интерфейсы использовали
        Set<T> usedInterfaces = new HashSet<T>(); Set<PropertyMapImplement<?,T>> usedProperties = new HashSet<PropertyMapImplement<?,T>>();
        for(PropertyInterfaceImplement<T> interfaceImplement : implement.mapping.values())
            interfaceImplement.fill(usedInterfaces, usedProperties);
        for(PropertyMapImplement<?, T> usedProperty : usedProperties)
            usedInterfaces.addAll(usedProperty.mapping.values());

        // создаем свойство - перемаппим интерфейсы
        JoinProperty<L> joinProperty = new JoinProperty<L>(genID(),"sys",usedInterfaces.size(),false);
        Map<T,JoinProperty.Interface> joinMap = BaseUtils.buildMap(usedInterfaces,joinProperty.interfaces); // строим карту

        joinProperty.implement = new PropertyImplement<PropertyInterfaceImplement<JoinProperty.Interface>,L>(implement.property,mapImplements(implement.mapping,joinMap));
        return new PropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createRestCompare(Collection<T> interfaces, PropertyMapImplement<?,T> distribute, PropertyMapImplement<?,T> previous) {
        JoinProperty<CompareFormulaProperty.Interface> joinProperty = new JoinProperty<CompareFormulaProperty.Interface>(genID(),"sys",interfaces.size(),false);
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(interfaces, joinProperty.interfaces);

        compareJoin(joinProperty, Compare.GREATER, distribute.map(joinMap), previous.map(joinMap));
        
        return new PropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createRestMin(Property<T> restriction, PropertyMapImplement<?,T> distribute, PropertyMapImplement<?,T> previous) {
        JoinProperty<StringFormulaProperty.Interface> joinProperty = new JoinProperty<StringFormulaProperty.Interface>(genID(),"sys",restriction.interfaces.size(),false);
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(restriction.interfaces, joinProperty.interfaces);

        StringFormulaProperty implement = new StringFormulaProperty(genID(),formulaClass,"prm1+prm2-prm3-GREATEST(prm2-prm3,prm1)",3); // так извращенно для инварианта определенности 
        Map<StringFormulaProperty.Interface,PropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = new HashMap<StringFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>();
        joinImplement.put(implement.findInterface("prm1"),new PropertyMapImplement<T,JoinProperty.Interface>(restriction,joinMap));
        joinImplement.put(implement.findInterface("prm2"),distribute.map(joinMap));
        joinImplement.put(implement.findInterface("prm3"),previous.map(joinMap));

        joinProperty.implement = new PropertyImplement<PropertyInterfaceImplement<JoinProperty.Interface>,StringFormulaProperty.Interface>(implement,joinImplement);
        return new PropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createRestResult(Collection<T> interfaces, PropertyMapImplement<?,T> restMin, PropertyMapImplement<?,T> restCompare) {
        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<AndFormulaProperty.Interface>(genID(),"sys",interfaces.size(),false);
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(interfaces, joinProperty.interfaces);

        AndFormulaProperty implement = new AndFormulaProperty(genID(),false);
        Map<AndFormulaProperty.Interface,PropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = new HashMap<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>();
        joinImplement.put(implement.objectInterface,restMin.map(joinMap));
        joinImplement.put(implement.andInterfaces.iterator().next(),restCompare.map(joinMap));

        joinProperty.implement = new PropertyImplement<PropertyInterfaceImplement<JoinProperty.Interface>,AndFormulaProperty.Interface>(implement,joinImplement);
        return new PropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createFirstMin(Property<T> restriction, PropertyMapImplement<?,T> distribute) {
        JoinProperty<StringFormulaProperty.Interface> joinProperty = new JoinProperty<StringFormulaProperty.Interface>(genID(),"sys",restriction.interfaces.size(),false);
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(restriction.interfaces, joinProperty.interfaces);

        StringFormulaProperty implement = new StringFormulaProperty(genID(),formulaClass,"MIN(prm1,prm2)",2);
        Map<StringFormulaProperty.Interface,PropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = new HashMap<StringFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>();
        joinImplement.put(implement.findInterface("prm1"),new PropertyMapImplement<T,JoinProperty.Interface>(restriction,joinMap));
        joinImplement.put(implement.findInterface("prm2"),distribute.map(joinMap));

        joinProperty.implement = new PropertyImplement<PropertyInterfaceImplement<JoinProperty.Interface>,StringFormulaProperty.Interface>(implement,joinImplement);
        return new PropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createIsFirst(Collection<T> interfaces, PropertyMapImplement<?,T> firstMin, PropertyMapImplement<?,T> previous) {
        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<AndFormulaProperty.Interface>(genID(),"sys",interfaces.size(),false);
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(interfaces, joinProperty.interfaces);

        AndFormulaProperty implement = new AndFormulaProperty(genID(),true);
        Map<AndFormulaProperty.Interface,PropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = new HashMap<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>();
        joinImplement.put(implement.objectInterface,firstMin.map(joinMap));
        joinImplement.put(implement.andInterfaces.iterator().next(),previous.map(joinMap));

        joinProperty.implement = new PropertyImplement<PropertyInterfaceImplement<JoinProperty.Interface>,AndFormulaProperty.Interface>(implement,joinImplement);
        return new PropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    public static ClassProperty createStatic(Object value, ConcreteValueClass valueClass) {
        return new ClassProperty(genID(),"sys",new ValueClass[]{}, valueClass, value); 
    }

    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createZero(Property<T> restriction) {
        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<AndFormulaProperty.Interface>(genID(),"sys",restriction.interfaces.size(),false);
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(restriction.interfaces, joinProperty.interfaces);

        AndFormulaProperty implement = new AndFormulaProperty(genID(),false);
        Map<AndFormulaProperty.Interface,PropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = new HashMap<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>>();
        joinImplement.put(implement.objectInterface,new PropertyMapImplement<ClassPropertyInterface,JoinProperty.Interface>(createStatic(0, formulaClass)));
        joinImplement.put(implement.andInterfaces.iterator().next(),new PropertyMapImplement<T,JoinProperty.Interface>(restriction,joinMap));

        joinProperty.implement = new PropertyImplement<PropertyInterfaceImplement<JoinProperty.Interface>,AndFormulaProperty.Interface>(implement,joinImplement);
        return new PropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createUnion(Collection<T> interfaces, PropertyMapImplement<?,T> first, PropertyMapImplement<?,T> rest) {
        OverrideUnionProperty unionProperty = new OverrideUnionProperty(genID(),"sys",interfaces.size());
        Map<T,UnionProperty.Interface> mapInterfaces = BaseUtils.buildMap(interfaces,unionProperty.interfaces);

        unionProperty.operands.add(first.map(mapInterfaces));
        unionProperty.operands.add(rest.map(mapInterfaces));

        return new PropertyMapImplement<UnionProperty.Interface,T>(unionProperty,BaseUtils.reverse(mapInterfaces));
    }

    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createDiff(Property<T> restriction, PropertyMapImplement<?,T> from) {
        SumUnionProperty unionProperty = new SumUnionProperty(genID(),"sys",restriction.interfaces.size());
        Map<T,UnionProperty.Interface> mapInterfaces = BaseUtils.buildMap(restriction.interfaces,unionProperty.interfaces);

        unionProperty.operands.put(from.map(mapInterfaces),1);
        unionProperty.operands.put(new PropertyMapImplement<T,UnionProperty.Interface>(restriction,mapInterfaces),-1);

        return new PropertyMapImplement<UnionProperty.Interface,T>(unionProperty,BaseUtils.reverse(mapInterfaces));
    }

    public static <L extends PropertyInterface, T extends PropertyInterface> PropertyMapImplement<?,T> createUGProp(PropertyImplement<PropertyInterfaceImplement<T>,L> group, OrderedMap<PropertyInterfaceImplement<T>,Boolean> orders, Property<T> restriction) {
        // assert'им что все порядки есть, иначе неправильно расписываться будет
        assert BaseUtils.mergeSet(orders.keySet(),BaseUtils.reverse(group.mapping).keySet()).containsAll(restriction.interfaces);

        // нужно MIN(огр., распр. - пред.) И распр. > пред. причем пред. подходит и null, остальные не null
        // кроме того у огр. и пред. есть все интерфейсы, а у распр. - не все
        // старый вариант : пока  MIN(огр., распр. - пред.) И распр. > пред. (1) ИЛИ MIN(огр., распр.) И пред. null (2)
        // новый вариант : пред. = UNION (0 and огр., сум. без) или считаем (суи. с - огр.) = пред. ??? и зтем обычную формулу

        // считаем пред., тут 2 варианта
        PropertyMapImplement<?, T> previous;
        // через Union
        if(false) {
            // сум. без
            PropertyMapImplement<?, T> orderSum = createOProp(restriction, group.mapping.values(), orders, false);

            // 0 and огр
            PropertyMapImplement<?, T> firstZero = createZero(restriction);

            // UNION(0 and огр, сум. без)
            previous = createUnion(restriction.interfaces, firstZero, orderSum);
        } else {
            // сум. с
            PropertyMapImplement<?, T> orderSum = createOProp(restriction, group.mapping.values(), orders, true);

            // сум. с - огр.
            previous = createDiff(restriction, orderSum);
        }

        // строим связное distribute св-во, узнаем все использованные интерфейсы, строим map
        PropertyMapImplement<?, T> distribute = createJoinProp(group);

        // распр. > пред.
        PropertyMapImplement<?, T> compare = createRestCompare(restriction.interfaces, distribute, previous);

        // MIN(огр., распр. - пред.)
        PropertyMapImplement<?, T> min = createRestMin(restriction, distribute, previous);

        // MIN(огр., распр. - пред.) И распр. > пред.
        return createRestResult(restriction.interfaces, min, compare);
    }
}
