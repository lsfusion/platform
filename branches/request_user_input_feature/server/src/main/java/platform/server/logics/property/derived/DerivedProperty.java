package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.identity.DefaultSIDGenerator;
import platform.interop.Compare;
import platform.server.classes.*;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.PartitionType;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.flow.IfActionProperty;
import platform.server.logics.property.actions.flow.SetActionProperty;

import java.util.*;

import static platform.base.BaseUtils.toList;

public class DerivedProperty {
    public static final String ID_PREFIX_GEN = "GDVID";
    private static final DefaultSIDGenerator sidGenerator = new DefaultSIDGenerator(ID_PREFIX_GEN);

    public static String genID() {
        return sidGenerator.genSID();
    }

    private static StaticClass formulaClass = DoubleClass.instance;

    // общие методы

    private static CalcPropertyImplement<CompareFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>> compareJoin(Compare compare, CalcPropertyInterfaceImplement<JoinProperty.Interface> operator1, CalcPropertyInterfaceImplement<JoinProperty.Interface> operator2) {
        CompareFormulaProperty compareProperty = new CompareFormulaProperty(genID(),compare);

        Map<CompareFormulaProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>> mapImplement = new HashMap<CompareFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>();
        mapImplement.put(compareProperty.operator1, operator1);
        mapImplement.put(compareProperty.operator2, operator2);

        return new CalcPropertyImplement<CompareFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(compareProperty,mapImplement);
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> Collection<CalcPropertyInterfaceImplement<K>> mapImplements(Collection<? extends CalcPropertyInterfaceImplement<T>> interfaceImplements, Map<T,K> map) {
        Collection<CalcPropertyInterfaceImplement<K>> mapImplement = new ArrayList<CalcPropertyInterfaceImplement<K>>();
        for(CalcPropertyInterfaceImplement<T> interfaceImplementEntry : interfaceImplements)
            mapImplement.add(interfaceImplementEntry.map(map));
        return mapImplement;
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> List<CalcPropertyInterfaceImplement<K>> mapImplements(List<? extends CalcPropertyInterfaceImplement<T>> interfaceImplements, Map<T,K> map) {
        List<CalcPropertyInterfaceImplement<K>> mapImplement = new ArrayList<CalcPropertyInterfaceImplement<K>>();
        for(CalcPropertyInterfaceImplement<T> interfaceImplementEntry : interfaceImplements)
            mapImplement.add(interfaceImplementEntry.map(map));
        return mapImplement;
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface, C extends PropertyInterface> List<CalcPropertyMapImplement<C, K>> mapCalcImplements(Map<T, K> map, List<CalcPropertyMapImplement<C, T>> propertyImplements) {
        List<CalcPropertyMapImplement<C, K>> mapImplement = new ArrayList<CalcPropertyMapImplement<C, K>>();
        for(CalcPropertyMapImplement<C, T> interfaceImplementEntry : propertyImplements)
            mapImplement.add(interfaceImplementEntry.map(map));
        return mapImplement;
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> List<ActionPropertyMapImplement<K>> mapActionImplements(Map<T,K> map, List<ActionPropertyMapImplement<T>> propertyImplements) {
        List<ActionPropertyMapImplement<K>> mapImplement = new ArrayList<ActionPropertyMapImplement<K>>();
        for(ActionPropertyMapImplement<T> interfaceImplementEntry : propertyImplements)
            mapImplement.add(interfaceImplementEntry.map(map));
        return mapImplement;
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> Map<L,CalcPropertyInterfaceImplement<K>> mapImplements(Map<L,CalcPropertyInterfaceImplement<T>> interfaceImplements, Map<T,K> map) {
        Map<L,CalcPropertyInterfaceImplement<K>> mapImplement = new HashMap<L, CalcPropertyInterfaceImplement<K>>();
        for(Map.Entry<L,CalcPropertyInterfaceImplement<T>> interfaceImplementEntry : interfaceImplements.entrySet())
            mapImplement.put(interfaceImplementEntry.getKey(),interfaceImplementEntry.getValue().map(map));
        return mapImplement;
    }

    public static <T extends PropertyInterface,K extends PropertyInterface> ActionPropertyImplement<CalcPropertyInterfaceImplement<K>> mapActionImplements(ActionPropertyImplement<CalcPropertyInterfaceImplement<T>> implement, Map<T,K> map) {
        return new ActionPropertyImplement<CalcPropertyInterfaceImplement<K>>(implement.property,mapImplements(implement.mapping,map));
    }

    // фильтрует только используемые интерфейсы и создает Join свойство с mapping'ом на эти интерфейсы
    public static <L extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<?,T> createJoin(CalcPropertyImplement<L, CalcPropertyInterfaceImplement<T>> implement) {
        // определяем какие интерфейсы использовали
        Set<T> usedInterfaces = new HashSet<T>(); Set<CalcPropertyMapImplement<?,T>> usedProperties = new HashSet<CalcPropertyMapImplement<?,T>>();
        for(CalcPropertyInterfaceImplement<T> interfaceImplement : implement.mapping.values())
            interfaceImplement.fill(usedInterfaces, usedProperties);
        for(CalcPropertyMapImplement<?, T> usedProperty : usedProperties)
            usedInterfaces.addAll(usedProperty.mapping.values());

        // создаем свойство - перемаппим интерфейсы
        List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(usedInterfaces.size());
        Map<T,JoinProperty.Interface> joinMap = BaseUtils.buildMap(usedInterfaces, listInterfaces); // строим карту
        JoinProperty<L> joinProperty = new JoinProperty<L>(genID(),"sys", listInterfaces ,false,
                new CalcPropertyImplement<L, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(implement.property,mapImplements(implement.mapping,joinMap)));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createCompare(Compare compare, T operator1, T operator2) {
        CompareFormulaProperty compareProperty = new CompareFormulaProperty(genID(),compare);

        Map<CompareFormulaProperty.Interface,T> mapImplement = new HashMap<CompareFormulaProperty.Interface, T>();
        mapImplement.put(compareProperty.operator1, operator1);
        mapImplement.put(compareProperty.operator2, operator2);

        return new CalcPropertyMapImplement<CompareFormulaProperty.Interface, T>(compareProperty,mapImplement);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createCompare(String caption, Collection<T> interfaces, CalcPropertyInterfaceImplement<T> distribute, CalcPropertyInterfaceImplement<T> previous, Compare compare) {
        List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(interfaces.size());
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(interfaces, listInterfaces);
        JoinProperty<CompareFormulaProperty.Interface> joinProperty = new JoinProperty<CompareFormulaProperty.Interface>(genID(),caption,
                listInterfaces,false, compareJoin(compare, distribute.map(joinMap), previous.map(joinMap)));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createCompare(Collection<T> interfaces, CalcPropertyInterfaceImplement<T> distribute, CalcPropertyInterfaceImplement<T> previous, Compare compare) {
        return createCompare("sys", interfaces, distribute, previous, compare);
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(String name, String caption, Collection<T> interfaces, CalcPropertyInterfaceImplement<T> object, List<CalcPropertyInterfaceImplement<T>> ands, List<Boolean> nots) {
        if(ands.size()==0 && object instanceof CalcPropertyMapImplement)
            return (CalcPropertyMapImplement<?,T>)object;

        List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(interfaces.size());
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(interfaces, listInterfaces);

        AndFormulaProperty implement = new AndFormulaProperty(genID(),BaseUtils.convertArray(nots.toArray(new Boolean[nots.size()])));
        Map<AndFormulaProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = new HashMap<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>();
        joinImplement.put(implement.objectInterface,object.map(joinMap));
        Iterator<AndFormulaProperty.AndInterface> andIterator = implement.andInterfaces.iterator();
        for(CalcPropertyInterfaceImplement<T> and : ands)
            joinImplement.put(andIterator.next(),and.map(joinMap));

        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<AndFormulaProperty.Interface>(name,caption,
                listInterfaces,false,new CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(implement,joinImplement));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(Collection<T> interfaces, CalcPropertyInterfaceImplement<T> object, List<CalcPropertyInterfaceImplement<T>> ands, List<Boolean> nots) {
        return createAnd(genID(), "sys", interfaces, object, ands, nots);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(Collection<T> interfaces, CalcPropertyInterfaceImplement<T> object, Collection<? extends CalcPropertyInterfaceImplement<T>> ands) {
        return createAnd(genID(), "sys", interfaces, object, ands);
    }
    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(String name, String caption, Collection<T> interfaces, CalcPropertyInterfaceImplement<T> object, Collection<? extends CalcPropertyInterfaceImplement<T>> ands) {
        List<CalcPropertyInterfaceImplement<T>> andList = new ArrayList<CalcPropertyInterfaceImplement<T>>();
        List<Boolean> andNots = new ArrayList<Boolean>();
        for(CalcPropertyInterfaceImplement<T> and : ands) {
            andList.add(and);
            andNots.add(false);
        }
        return createAnd(name, caption, interfaces, object, andList, andNots);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAndNot(CalcProperty<T> property, CalcPropertyInterfaceImplement<T> not) {
        return createAnd(genID(), "sys", property.interfaces, property.getImplement(), Collections.singletonList(not), Collections.singletonList(true));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(CalcProperty<T> property, CalcPropertyInterfaceImplement<T> and) {
        return createAnd(property.interfaces, property.getImplement(), Collections.singleton(and));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(Collection<T> interfaces, CalcPropertyInterfaceImplement<T> object, CalcPropertyInterfaceImplement<T> and) {
        return createAnd(interfaces, object, Collections.singleton(and));
    }

    public static <T extends PropertyInterface, C extends PropertyInterface> CalcPropertyMapImplement<?,T> createUnion(Collection<T> interfaces, List<CalcPropertyMapImplement<?, T>> props) {
        List<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(interfaces.size());
        Map<T,UnionProperty.Interface> mapInterfaces = BaseUtils.buildMap(interfaces,listInterfaces);

        List<CalcPropertyMapImplement<?,UnionProperty.Interface>> operands =
                BaseUtils.<List<CalcPropertyMapImplement<?, UnionProperty.Interface>>>immutableCast(DerivedProperty.mapCalcImplements(mapInterfaces,
                        BaseUtils.<List<CalcPropertyMapImplement<C, T>>>immutableCast(props)));
        OverrideUnionProperty unionProperty = new OverrideUnionProperty(genID(),"sys",listInterfaces,operands);
        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(unionProperty,BaseUtils.reverse(mapInterfaces));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createUnion(Collection<T> interfaces, CalcPropertyMapImplement<?,T> first, CalcPropertyMapImplement<?,T> rest) {
        return createUnion(interfaces, BaseUtils.toList(first, rest));
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createDiff(CalcProperty<T> restriction, CalcPropertyMapImplement<?,T> from) {
        List<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(restriction.interfaces.size());
        Map<T,UnionProperty.Interface> mapInterfaces = BaseUtils.buildMap(restriction.interfaces,listInterfaces);

        Map<CalcPropertyMapImplement<?, UnionProperty.Interface>, Integer> operands = new HashMap<CalcPropertyMapImplement<?, UnionProperty.Interface>, Integer>();
        operands.put(from.map(mapInterfaces),1);
        operands.put(new CalcPropertyMapImplement<T,UnionProperty.Interface>(restriction,mapInterfaces),-1);
        SumUnionProperty unionProperty = new SumUnionProperty(genID(),"sys",listInterfaces,operands);
        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(unionProperty,BaseUtils.reverse(mapInterfaces));
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createSum(String sID, String caption, Collection<T> interfaces, CalcPropertyMapImplement<?, T> sum1, CalcPropertyMapImplement<?,T> sum2) {
        List<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(interfaces.size());
        Map<T,UnionProperty.Interface> mapInterfaces = BaseUtils.buildMap(interfaces,listInterfaces);

        Map<CalcPropertyMapImplement<?, UnionProperty.Interface>, Integer> operands = new HashMap<CalcPropertyMapImplement<?, UnionProperty.Interface>, Integer>();
        operands.put(sum1.map(mapInterfaces),1);
        operands.put(sum2.map(mapInterfaces),1);
        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(new SumUnionProperty(sID,caption,listInterfaces,operands),BaseUtils.reverse(mapInterfaces));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createStatic(Object value, StaticClass valueClass) {
        return new CalcPropertyMapImplement<PropertyInterface,T>(new ValueProperty(genID(),"sys", value, valueClass),new HashMap<PropertyInterface, T>());
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createFormula(Collection<T> interfaces, String formula, ConcreteValueClass valueClass, List<? extends CalcPropertyInterfaceImplement<T>> params) {
        return createFormula(genID(), "sys", interfaces, formula, valueClass, params);
    }
    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createFormula(String sID, String caption, Collection<T> interfaces, String formula, ConcreteValueClass valueClass, List<? extends CalcPropertyInterfaceImplement<T>> params) {
        List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(interfaces.size());
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(interfaces, listInterfaces);

        StringFormulaProperty implement = new StringFormulaProperty(genID(),valueClass,formula,params.size());
        Map<StringFormulaProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = new HashMap<StringFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>();
        for(int i=0;i<params.size();i++)
            joinImplement.put(implement.findInterface("prm"+(i+1)),params.get(i).map(joinMap));

        JoinProperty<StringFormulaProperty.Interface> joinProperty = new JoinProperty<StringFormulaProperty.Interface>(sID,caption,
                listInterfaces,false,new CalcPropertyImplement<StringFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(implement,joinImplement));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createConcatenate(String sID, String caption, Collection<T> interfaces, List<? extends CalcPropertyInterfaceImplement<T>> params) {
        List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(interfaces.size());
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(interfaces, listInterfaces);

        ConcatenateProperty implement = new ConcatenateProperty(genID(), params.size());
        Map<ConcatenateProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = new HashMap<ConcatenateProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>();
        for(int i=0;i<params.size();i++)
            joinImplement.put(implement.getInterface(i),params.get(i).map(joinMap));

        JoinProperty<ConcatenateProperty.Interface> joinProperty = new JoinProperty<ConcatenateProperty.Interface>(sID, caption,
                listInterfaces, false, new CalcPropertyImplement<ConcatenateProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(implement,joinImplement));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createDeconcatenate(String sID, String caption, CalcProperty<T> property, int part, BaseClass baseClass) {
        List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(property.interfaces.size());
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(property.interfaces, listInterfaces);

        DeconcatenateProperty implement = new DeconcatenateProperty(genID(),part,baseClass);
        Map<DeconcatenateProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = Collections.<DeconcatenateProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>>singletonMap(BaseUtils.single(implement.interfaces),new CalcPropertyMapImplement<T,JoinProperty.Interface>(property,joinMap));

        JoinProperty<DeconcatenateProperty.Interface> joinProperty = new JoinProperty<DeconcatenateProperty.Interface>(sID,caption,listInterfaces,false,
                new CalcPropertyImplement<DeconcatenateProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(implement,joinImplement));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    // PARTITION методы

    // создает "раздваивающее" свойство (self join в SQL)

    private static <T extends PropertyInterface,P extends PropertyInterface> List<JoinProperty.Interface> createSelfProp(Collection<T> interfaces, Map<T,JoinProperty.Interface> map1, Map<T,JoinProperty.Interface> map2, Set<T> partInterfaces) {
        List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(interfaces.size() * 2 - partInterfaces.size());
        Iterator<JoinProperty.Interface> joinIterator = listInterfaces.iterator();
        for(T propertyInterface : interfaces) {
            JoinProperty.Interface joinInterface = joinIterator.next();
            map1.put(propertyInterface, joinInterface);
            map2.put(propertyInterface, (partInterfaces.contains(propertyInterface)?joinInterface:joinIterator.next()));
        }
        return listInterfaces;
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<?,JoinProperty.Interface> createCompareProp(CalcPropertyMapImplement<P,T> implement, Map<T,JoinProperty.Interface> map1, Map<T,JoinProperty.Interface> map2, Compare compare) {
        Map<P,JoinProperty.Interface> join1 = new HashMap<P, JoinProperty.Interface>(); Map<P,JoinProperty.Interface> join2 = new HashMap<P, JoinProperty.Interface>();
        List<JoinProperty.Interface> listInterfaces = createSelfProp(implement.property.interfaces, join1, join2, new HashSet<P>());
        JoinProperty<CompareFormulaProperty.Interface> joinProperty = new JoinProperty<CompareFormulaProperty.Interface>(genID(),"sys",listInterfaces,false,
                compareJoin(compare, new CalcPropertyMapImplement<P, JoinProperty.Interface>(implement.property, join1), new CalcPropertyMapImplement<P, JoinProperty.Interface>(implement.property, join2)));
        return new CalcPropertyMapImplement<JoinProperty.Interface,JoinProperty.Interface>(joinProperty, BaseUtils.merge(
                BaseUtils.crossJoin(join1,BaseUtils.join(implement.mapping,map1)),
                BaseUtils.crossJoin(join2,BaseUtils.join(implement.mapping,map2))));
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<?,JoinProperty.Interface> createInterfaceCompareMap(T implement, Map<T,JoinProperty.Interface> map1, Map<T,JoinProperty.Interface> map2, Compare compare) {
        CompareFormulaProperty compareProperty = new CompareFormulaProperty(genID(),compare);

        Map<CompareFormulaProperty.Interface,JoinProperty.Interface> mapImplement = new HashMap<CompareFormulaProperty.Interface, JoinProperty.Interface>();
        mapImplement.put(compareProperty.operator1, map1.get(implement));
        mapImplement.put(compareProperty.operator2, map2.get(implement));

        return new CalcPropertyMapImplement<CompareFormulaProperty.Interface,JoinProperty.Interface>(compareProperty,mapImplement);
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<?,JoinProperty.Interface> createCompareMap(CalcPropertyInterfaceImplement<T> implement, Map<T,JoinProperty.Interface> map1, Map<T,JoinProperty.Interface> map2, Compare compare) {
        if(implement instanceof CalcPropertyMapImplement)
            return createCompareProp((CalcPropertyMapImplement<?,T>)implement, map1, map2, compare);
        else
            return createInterfaceCompareMap((T)implement, map1, map2, compare);
    }

    public static <T extends PropertyInterface> JoinProperty<AndFormulaProperty.Interface> createPartition(Collection<T> interfaces, CalcPropertyInterfaceImplement<T> property, Collection<CalcPropertyInterfaceImplement<T>> partitions, CalcPropertyInterfaceImplement<T> expr, Map<T, JoinProperty.Interface> mapMain, Compare compare) {
        // "двоим" интерфейсы (для partition'а не PropertyInterface), для результ. св-ва
        Set<T> partInterfaces = new HashSet<T>();
        Set<CalcPropertyMapImplement<?,T>> partProperties = new HashSet<CalcPropertyMapImplement<?,T>>();
        for(CalcPropertyInterfaceImplement<T> partition : partitions)
            partition.fill(partInterfaces, partProperties);

        Map<T,JoinProperty.Interface> mapDupl = new HashMap<T, JoinProperty.Interface>();
        List<JoinProperty.Interface> listInterfaces = createSelfProp(interfaces, mapMain, mapDupl, partInterfaces);

        // ставим equals'ы на partitions свойства (раздвоенные), greater на предшествие order (раздвоенное)
        AndFormulaProperty andPrevious = new AndFormulaProperty(genID(),new boolean[partProperties.size()+1]);
        Map<AndFormulaProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>> mapImplement = new HashMap<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>();
        mapImplement.put(andPrevious.objectInterface,property.map(mapDupl));
        Iterator<AndFormulaProperty.AndInterface> itAnd = andPrevious.andInterfaces.iterator();
        for(CalcPropertyMapImplement<?, T> partProperty : partProperties)
            mapImplement.put(itAnd.next(), createCompareProp(partProperty, mapMain, mapDupl, Compare.EQUALS));
        mapImplement.put(itAnd.next(),createCompareMap(expr, mapMain, mapDupl, compare));

        return new JoinProperty<AndFormulaProperty.Interface>(genID(),"sys",listInterfaces,false,
                new CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(andPrevious, mapImplement));
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createSOProp(CalcProperty<T> property, Collection<CalcPropertyInterfaceImplement<T>> partitions, CalcPropertyInterfaceImplement<T> order, boolean ascending, boolean last) {
        return createSOProp(genID(), "sys", property, partitions, order, ascending, last);
    }
    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createSOProp(String sID, String caption, CalcProperty<T> property, Collection<CalcPropertyInterfaceImplement<T>> partitions, CalcPropertyInterfaceImplement<T> order, boolean ascending, boolean last) {

        Map<T,JoinProperty.Interface> mapMain = new HashMap<T, JoinProperty.Interface>();
        JoinProperty<AndFormulaProperty.Interface> joinProperty = createPartition(property.interfaces, property.getImplement(), partitions, order, mapMain,
                ascending ? (last ? Compare.LESS_EQUALS : Compare.LESS) : (last ? Compare.GREATER_EQUALS : Compare.GREATER));

        // создаем groupProperty для map2 и даем их на выход
        SumGroupProperty<JoinProperty.Interface> groupProperty = new SumGroupProperty<JoinProperty.Interface>(sID, caption, mapMain.values(), joinProperty);
        Map<GroupProperty.Interface<JoinProperty.Interface>,JoinProperty.Interface> mapInterfaces = BaseUtils.immutableCast(groupProperty.getMapInterfaces());
        return new CalcPropertyMapImplement<GroupProperty.Interface<JoinProperty.Interface>,T>(groupProperty,BaseUtils.join(mapInterfaces,BaseUtils.reverse(mapMain)));
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createOProp(CalcProperty<T> property, Collection<CalcPropertyInterfaceImplement<T>> partitions, OrderedMap<CalcPropertyInterfaceImplement<T>,Boolean> orders, boolean includeLast) {
        return createOProp(genID(), "sys", PartitionType.SUM, property, partitions, orders, includeLast);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createOProp(String sID, String caption, PartitionType partitionType, CalcProperty<T> property, Collection<CalcPropertyInterfaceImplement<T>> partitions, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean includeLast) {
        if(true) {
            List<CalcPropertyInterfaceImplement<T>> propList = Collections.<CalcPropertyInterfaceImplement<T>>singletonList(property.getImplement());
            return createOProp(sID, caption, partitionType, property.interfaces, propList, partitions, orders, includeLast);
        }

        assert orders.size()>0;
        
        if(orders.size()==1) return createSOProp(sID, caption, property, partitions, orders.singleKey(), orders.singleValue(), includeLast);
        // итеративно делаем Union, перекидывая order'ы в partition'ы
        List<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(property.interfaces.size());
        Map<T,UnionProperty.Interface> mapInterfaces = BaseUtils.buildMap(property.interfaces,listInterfaces);

        Map<CalcPropertyMapImplement<?, UnionProperty.Interface>, Integer> operands = new HashMap<CalcPropertyMapImplement<?, UnionProperty.Interface>, Integer>();
        Iterator<Map.Entry<CalcPropertyInterfaceImplement<T>,Boolean>> it = orders.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<CalcPropertyInterfaceImplement<T>,Boolean> order = it.next();
            operands.put(createSOProp(property, partitions, order.getKey(), order.getValue(), it.hasNext() && includeLast).map(mapInterfaces),1);
            partitions = new ArrayList<CalcPropertyInterfaceImplement<T>>(partitions);
            partitions.add(order.getKey());
        }

        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(new SumUnionProperty(sID,caption,listInterfaces,operands),BaseUtils.reverse(mapInterfaces));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createOProp(String sID, String caption, PartitionType partitionType, Collection<T> innerInterfaces, List<CalcPropertyInterfaceImplement<T>> props, Collection<CalcPropertyInterfaceImplement<T>> partitions, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean includeLast) {
        PartitionProperty<T> orderProperty = new PartitionProperty<T>(sID, caption, partitionType, innerInterfaces, props, partitions, orders, includeLast);
        return new CalcPropertyMapImplement<PartitionProperty.Interface<T>,T>(orderProperty,orderProperty.getMapInterfaces());
    }

    public static <T> CalcPropertyImplement<?,T> createCProp(StaticClass valueClass, Object value, Map<T, ValueClass> params) {
        return createCProp(genID(), "sys", valueClass, value, params);
    }

    public static <T> CalcPropertyImplement<?,T> createCProp(String sID, String caption, StaticClass valueClass, Object value, Map<T, ValueClass> params) {
        return createCProp(sID, caption, valueClass instanceof LogicalClass && params.size() > 0 ? null : new ValueProperty(genID(), "sys", value, valueClass), params);
    }

    public static <T> CalcPropertyImplement<?,T> createCProp(String sID, String caption, DataClass valueClass, Map<T, ValueClass> params) {
        return createCProp(sID, caption, new InfiniteProperty(genID(), "sys", valueClass), params);
    }

    public static <T,V extends PropertyInterface> CalcPropertyImplement<?,T> createCProp(String sID, String caption, CalcProperty<V> valueProperty, Map<T, ValueClass> params) {
        if(params.size()==0)
            return new CalcPropertyImplement<V, T>(valueProperty, new HashMap<V, T>());

        Map<PropertyInterface, T> mapInterfaces = new HashMap<PropertyInterface, T>();
        List<CalcPropertyInterfaceImplement<PropertyInterface>> listImplements = new ArrayList<CalcPropertyInterfaceImplement<PropertyInterface>>();
        for(Map.Entry<T, ValueClass> param : params.entrySet()) {
            PropertyInterface propertyInterface = new PropertyInterface();
            listImplements.add(IsClassProperty.getProperty(param.getValue(), "value").mapPropertyImplement(Collections.singletonMap("value", propertyInterface)));
            mapInterfaces.put(propertyInterface, param.getKey());
        }

        CalcPropertyMapImplement<?, PropertyInterface> and;
        if(valueProperty==null)
            and = createAnd(sID, caption, mapInterfaces.keySet(), listImplements.get(0), listImplements.subList(1, listImplements.size()));
        else
            and = createAnd(sID, caption, mapInterfaces.keySet(), new CalcPropertyMapImplement<V, PropertyInterface>(valueProperty), listImplements);
        return and.mapImplement(mapInterfaces);
    }

    
    // строится partition с пустым order'ом
    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createPProp(Collection<T> innerInterfaces, CalcPropertyInterfaceImplement<T> property, Collection<CalcPropertyInterfaceImplement<T>> partitions, GroupType type) {
        GroupProperty<T> partitionGroup = type.createProperty(genID(), "sys", innerInterfaces, property, partitions);
        return createJoin(new CalcPropertyImplement<GroupProperty.Interface<T>, CalcPropertyInterfaceImplement<T>>(partitionGroup,partitionGroup.getMapInterfaces()));
    }

    private static <L extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<?,T> createPProp(CalcPropertyMapImplement<L, T> mapImplement, Collection<CalcPropertyInterfaceImplement<T>> partitions, GroupType type) {
        // assert что все интерфейсы есть
        return createPProp(mapImplement.property.interfaces, mapImplement.property.getImplement(), mapImplements(partitions, BaseUtils.reverse(mapImplement.mapping)), type).map(mapImplement.mapping);
    }

    public static <L extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<?,T> createUGProp(CalcPropertyImplement<L, CalcPropertyInterfaceImplement<T>> group, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, CalcProperty<T> restriction, boolean over) {
        return createUGProp(genID(), "sys", restriction.interfaces, group, orders, restriction.getImplement(), over);
    }
    public static <L extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<?,T> createUGProp(String sID, String caption, Collection<T> innerInterfaces, CalcPropertyImplement<L, CalcPropertyInterfaceImplement<T>> group, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, CalcPropertyInterfaceImplement<T> restriction, boolean over) {
        Collection<CalcPropertyInterfaceImplement<T>> partitions = group.mapping.values();

        // строим связное distribute св-во, узнаем все использованные интерфейсы, строим map
        CalcPropertyMapImplement<?, T> distribute = createJoin(group);

        if(true) {
            PartitionProperty<T> orderProperty = new PartitionProperty<T>(sID, caption, over ? PartitionType.DISTR_RESTRICT_OVER : PartitionType.DISTR_RESTRICT, innerInterfaces, toList(restriction, distribute), partitions, orders, true);
            return new CalcPropertyMapImplement<PartitionProperty.Interface<T>, T>(orderProperty, orderProperty.getMapInterfaces());
        }

        throw new RuntimeException("not supported");
/*
        // нужно MIN2(огр., распр. - пред.) И распр. > пред. причем пред. подходит и null, остальные не null
        // кроме того у огр. и пред. есть все интерфейсы, а у распр. - не все
        // старый вариант : пока  MIN2(огр., распр. - пред.) И распр. > пред. (1) ИЛИ MIN2(огр., распр.) И пред. null (2)
        // новый вариант : пред. = UNION (0 and огр., сум. без) или считаем (суи. с - огр.) = пред. ??? и зтем обычную формулу

        CalcPropertyMapImplement<?, T> restImplement = restriction.getImplement();

        // считаем пред., тут 2 варианта
        CalcPropertyMapImplement<?, T> previous;
        // через Union
        if(false) {
            // сум. без
            CalcPropertyMapImplement<?, T> orderSum = createOProp(restriction, group.mapping.values(), orders, false);

            // 0 and огр
            CalcPropertyMapImplement<?, T> firstZero = createAnd(restriction.interfaces, DerivedProperty.<T>createStatic(0, formulaClass), restImplement);

            // UNION(0 and огр, сум. без)
            previous = createUnion(restriction.interfaces, firstZero, orderSum);
        } else {
            // сум. с
            CalcPropertyMapImplement<?, T> orderSum = createOProp(restriction, group.mapping.values(), orders, true);

            // сум. с - огр.
            previous = createDiff(restriction, orderSum);
        }

        // MIN2(огр., распр. - пред.)
        CalcPropertyMapImplement<?, T> min = createFormula(restriction.interfaces, "(prm1+prm2-prm3-ABS(prm1-(prm2-prm3)))/2", formulaClass, BaseUtils.toList(restImplement, distribute, previous));

        // распр. > пред.
        CalcPropertyMapImplement<?, T> compare = createCompare(restriction.interfaces, distribute, previous, Compare.GREATER);

        // MIN2(огр., распр. - пред.) И распр. > пред.
        return createAnd(restriction.interfaces, min, compare);*/
    }

    public static <L extends PropertyInterface, T extends PropertyInterface<T>> CalcPropertyMapImplement<?,T> createPGProp(String sID, String caption, int roundlen, boolean roundfirst, BaseClass baseClass, Collection<T> innerInterfaces, CalcPropertyImplement<L, CalcPropertyInterfaceImplement<T>> group, CalcPropertyInterfaceImplement<T> proportion, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders) {

        Collection<CalcPropertyInterfaceImplement<T>> partitions = group.mapping.values();

        // строим partition distribute св-во
        CalcPropertyMapImplement<?, T> distribute = createJoin(group);

        if(roundfirst && true) {
            PartitionProperty<T> orderProperty = new PartitionProperty<T>(sID, caption, PartitionType.DISTR_CUM_PROPORTION, innerInterfaces, toList(proportion, distribute), partitions, orders, false);
            return new CalcPropertyMapImplement<PartitionProperty.Interface<T>, T>(orderProperty, orderProperty.getMapInterfaces());
        }

        // общая сумма по пропорции в partition'е
        CalcPropertyMapImplement<?, T> propSum = createPProp(innerInterfaces, proportion, group.mapping.values(), GroupType.SUM);

        String distrSID = !roundfirst ? sID : genID();
        String distrCaption = !roundfirst ? caption : "sys";
        // округляем
        CalcPropertyMapImplement<?, T> distrRound = createFormula(distrSID, distrCaption, innerInterfaces, "ROUND(CAST((prm1*prm2/prm3) as NUMERIC(15,3)),"+roundlen+")", formulaClass, toList(distribute, proportion, propSum));

        if (!roundfirst) return distrRound;
        
        throw new RuntimeException("not supported");

/*        // строим partition полученного округления
        CalcPropertyMapImplement<?, T> totRound = createPProp(distrRound, group.mapping.values(), GroupType.SUM);

        // получаем сколько надо дораспределить
        CalcPropertyMapImplement<?, T> diffRound = createFormula(proportion.interfaces, "prm1-prm2", formulaClass, BaseUtils.toList(distribute, totRound)); // вообще гря разные параметры поэтому formula, а не diff

        // берем первую пропорцию 
        CalcPropertyMapImplement<?, T> proportionFirst = createFOProp(proportion, baseClass, group.mapping.values());

        // включаем всю оставшуюся сумму на нее
        CalcPropertyMapImplement<?, T> diffFirst = createAnd(proportion.interfaces, diffRound, proportionFirst);

        // делаем union с основным distrRound'ом
        return createSum(sID, caption, proportion.interfaces, distrRound, diffFirst);*/
    }

    private static <T extends PropertyInterface> CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>> createMaxProp(String sID, String caption, Collection<T> interfaces, CalcPropertyInterfaceImplement<T> implement, Collection<CalcPropertyInterfaceImplement<T>> group, boolean min) {
        MaxGroupProperty<T> maxProperty = new MaxGroupProperty<T>(sID, caption, interfaces, group, implement, min);
        return new CalcPropertyImplement<GroupProperty.Interface<T>, CalcPropertyInterfaceImplement<T>>(maxProperty, maxProperty.getMapInterfaces());
    }
    
    public static <T extends PropertyInterface> List<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> createMGProp(String[] sIDs, String[] captions, Collection<T> interfaces, BaseClass baseClass, List<CalcPropertyInterfaceImplement<T>> props, Collection<CalcPropertyInterfaceImplement<T>> group, Collection<CalcProperty> persist, boolean min) {
        if(props.size()==1)
            return createEqualsMGProp(sIDs, captions, interfaces, props, group, persist, min);
        else
            return createConcMGProp(sIDs, captions, interfaces, baseClass, props, group, persist, min);
    }

    private static <T extends PropertyInterface> List<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> createEqualsMGProp(String[] sIDs, String[] captions, Collection<T> interfaces, List<CalcPropertyInterfaceImplement<T>> props, Collection<CalcPropertyInterfaceImplement<T>> group, Collection<CalcProperty> persist, boolean min) {
        CalcPropertyInterfaceImplement<T> propertyImplement = props.get(0);

        List<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> result = new ArrayList<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>>();
        int i = 1;
        do {
            CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>> maxImplement = createMaxProp(sIDs[i-1], captions[i-1], interfaces, propertyImplement, group, min);
            result.add(maxImplement);
            if(i<props.size()) { // если не последняя
                CalcPropertyMapImplement<?,T> prevMax = createJoin(maxImplement); // какой максимум в partition'е

                CalcPropertyMapImplement<?,T> equalsMax = createCompare(interfaces, propertyImplement, prevMax, Compare.EQUALS);

                propertyImplement = createAnd(interfaces, props.get(i), equalsMax);
            }
        } while (i++<props.size());
        return result;
    }

    private static <T extends PropertyInterface,L extends PropertyInterface> List<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> createDeconcatenate(String[] sIDs, String[] captions, CalcPropertyImplement<L, CalcPropertyInterfaceImplement<T>> implement, int parts, BaseClass baseClass) {
        List<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> result = new ArrayList<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>>();
        for(int i=0;i<parts;i++)
            result.add(createDeconcatenate(sIDs[i], captions[i], implement.property, i, baseClass).mapImplement(implement.mapping));
        return result;
    }

    private static <T extends PropertyInterface> List<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> createConcMGProp(String[] sIDs, String[] captions, Collection<T> interfaces, BaseClass baseClass, List<CalcPropertyInterfaceImplement<T>> props, Collection<CalcPropertyInterfaceImplement<T>> group, Collection<CalcProperty> persist, boolean min) {
        String concID = BaseUtils.toString("_", sIDs);
        String concCaption = BaseUtils.toString(", ", captions);

        CalcPropertyMapImplement<?, T> concate = createConcatenate("CC_" + concID, "Concatenate - "+ concCaption, interfaces, props);
        persist.add(concate.property);
        
        CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>> max = createMaxProp("MC_" + concID, "Concatenate - " + concCaption, interfaces, concate, group, min);
        persist.add(max.property);

        return createDeconcatenate(sIDs, captions, max, props.size(), baseClass);
    }
    
    public static <T extends PropertyInterface> CalcPropertyMapImplement<ClassPropertyInterface, T> createDataProp(boolean session, CalcProperty<T> property) {
        Map<T, ValueClass> interfaces = property.getMapClasses();
        ValueClass valueClass = property.getValueClass();
        ValueClass[] interfaceClasses = new ValueClass[interfaces.size()]; int iv = 0;
        List<T> listInterfaces = new ArrayList<T>();
        for(Map.Entry<T, ValueClass> entry : interfaces.entrySet()) {
            interfaceClasses[iv++] = entry.getValue();
            listInterfaces.add(entry.getKey());
        }
        String sID = "pc" + property.getSID(); String caption = property.toString();
        DataProperty dataProperty = (session ? new SessionDataProperty(sID, caption, interfaceClasses, valueClass) :
                        new StoredDataProperty(sID, caption, interfaceClasses, valueClass));
        return dataProperty.getImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<L> createSetAction(CalcProperty<L> property, boolean notNull, boolean check) {
        List<L> listInterfaces = new ArrayList<L>(property.interfaces);
        SetActionProperty<L, PropertyInterface, L> actionProperty = new SetActionProperty<L, PropertyInterface, L>(genID(), (notNull ? "Задать" : "Сбросить") + " " + property.caption,
                property.interfaces, listInterfaces, (CalcPropertyMapImplement<PropertyInterface,L>) DerivedProperty.createStatic(true, LogicalClass.instance), property.getImplement(), notNull, check);
        return actionProperty.getImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<L> createIfAction(Collection<L> innerInterfaces, CalcPropertyMapImplement<?, L> where, ActionPropertyMapImplement<L> action, ActionPropertyMapImplement<L> elseAction, boolean ifClasses) {
        List<L> listInterfaces = new ArrayList<L>(innerInterfaces);
        IfActionProperty actionProperty = new IfActionProperty(genID(), "sys", false, listInterfaces, where, action, elseAction, ifClasses);
        return actionProperty.getImplement(listInterfaces);
    }
}
