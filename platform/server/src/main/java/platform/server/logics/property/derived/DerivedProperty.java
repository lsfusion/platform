package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.identity.DefaultSIDGenerator;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.classes.*;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.PartitionType;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.AddObjectActionProperty;
import platform.server.logics.property.actions.ChangeClassActionProperty;
import platform.server.logics.property.actions.flow.*;

import java.util.*;

import static platform.base.BaseUtils.*;

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

    public static <L,T extends PropertyInterface,K extends PropertyInterface> OrderedMap<CalcPropertyInterfaceImplement<K>, Boolean> mapImplements(OrderedMap<? extends CalcPropertyInterfaceImplement<T>, Boolean> interfaceImplements, Map<T,K> map) {
        OrderedMap<CalcPropertyInterfaceImplement<K>, Boolean> mapImplement = new OrderedMap<CalcPropertyInterfaceImplement<K>, Boolean>();
        for(Map.Entry<? extends CalcPropertyInterfaceImplement<T>, Boolean> interfaceImplementEntry : interfaceImplements.entrySet())
            mapImplement.put(interfaceImplementEntry.getKey().map(map), interfaceImplementEntry.getValue());
        return mapImplement;
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface, C extends PropertyInterface> List<CalcPropertyInterfaceImplement<K>> mapCalcImplements(Map<T, K> map, List<CalcPropertyInterfaceImplement<T>> propertyImplements) {
        List<CalcPropertyInterfaceImplement<K>> mapImplement = new ArrayList<CalcPropertyInterfaceImplement<K>>();
        for(CalcPropertyInterfaceImplement<T> interfaceImplementEntry : propertyImplements)
            mapImplement.add(interfaceImplementEntry.map(map));
        return mapImplement;
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> List<ActionPropertyMapImplement<?, K>> mapActionImplements(Map<T,K> map, List<ActionPropertyMapImplement<?, T>> propertyImplements) {
        List<ActionPropertyMapImplement<?, K>> mapImplement = new ArrayList<ActionPropertyMapImplement<?, K>>();
        for(ActionPropertyMapImplement<?, T> interfaceImplementEntry : propertyImplements)
            mapImplement.add(interfaceImplementEntry.map(map));
        return mapImplement;
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> Map<L,CalcPropertyInterfaceImplement<K>> mapImplements(Map<L,CalcPropertyInterfaceImplement<T>> interfaceImplements, Map<T,K> map) {
        Map<L,CalcPropertyInterfaceImplement<K>> mapImplement = new HashMap<L, CalcPropertyInterfaceImplement<K>>();
        for(Map.Entry<L,CalcPropertyInterfaceImplement<T>> interfaceImplementEntry : interfaceImplements.entrySet())
            mapImplement.put(interfaceImplementEntry.getKey(),interfaceImplementEntry.getValue().map(map));
        return mapImplement;
    }

    public static <T extends PropertyInterface,K extends PropertyInterface, P extends PropertyInterface> ActionPropertyImplement<P, CalcPropertyInterfaceImplement<K>> mapActionImplements(ActionPropertyImplement<P, CalcPropertyInterfaceImplement<T>> implement, Map<T,K> map) {
        return new ActionPropertyImplement<P, CalcPropertyInterfaceImplement<K>>(implement.property,mapImplements(implement.mapping,map));
    }

    public static <T extends PropertyInterface> Collection<T> getUsedInterfaces(CalcPropertyInterfaceImplement<T> interfaceImplement) {
        if(interfaceImplement instanceof CalcPropertyMapImplement)
            return new HashSet<T>(((CalcPropertyMapImplement<?, T>)interfaceImplement).mapping.values());
        else
            return Collections.singleton((T)interfaceImplement);
    }

    public static <T extends PropertyInterface> Set<T> getUsedInterfaces(Collection<? extends CalcPropertyInterfaceImplement<T>> col) {
        Set<T> usedInterfaces = new HashSet<T>();
        for(CalcPropertyInterfaceImplement<T> interfaceImplement : col)
            usedInterfaces.addAll(getUsedInterfaces(interfaceImplement));
        return usedInterfaces;
    }

    // фильтрует только используемые интерфейсы и создает Join свойство с mapping'ом на эти интерфейсы
    public static <L extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<?,T> createJoin(CalcPropertyImplement<L, CalcPropertyInterfaceImplement<T>> implement) {
        // определяем какие интерфейсы использовали
        Set<T> usedInterfaces = getUsedInterfaces(implement.mapping.values());

        // создаем свойство - перемаппим интерфейсы
        List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(usedInterfaces.size());
        Map<T,JoinProperty.Interface> joinMap = BaseUtils.buildMap(usedInterfaces, listInterfaces); // строим карту
        JoinProperty<L> joinProperty = new JoinProperty<L>(genID(),"sys", listInterfaces ,false,
                new CalcPropertyImplement<L, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(implement.property,mapImplements(implement.mapping,joinMap)));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    public static <L extends PropertyInterface, T extends PropertyInterface> ActionPropertyMapImplement<?,T> createJoinAction(ActionPropertyImplement<L, CalcPropertyInterfaceImplement<T>> implement) {
        // определяем какие интерфейсы использовали
        Set<T> usedInterfaces = getUsedInterfaces(implement.mapping.values());

        // создаем свойство - перемаппим интерфейсы
        List<PropertyInterface> listInterfaces = JoinActionProperty.genInterfaces(usedInterfaces.size());
        Map<T,PropertyInterface> joinMap = BaseUtils.buildMap(usedInterfaces, listInterfaces); // строим карту
        JoinActionProperty<L> joinProperty = new JoinActionProperty<L>(genID(),"sys", listInterfaces,
                new ActionPropertyImplement<L, CalcPropertyInterfaceImplement<PropertyInterface>>(implement.property,mapImplements(implement.mapping,joinMap)));
        return new ActionPropertyMapImplement<PropertyInterface,T>(joinProperty,BaseUtils.reverse(joinMap));
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
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createCompare(CalcPropertyInterfaceImplement<T> distribute, CalcPropertyInterfaceImplement<T> previous, Compare compare) {
        return createCompare(getUsedInterfaces(toList(distribute, previous)), distribute, previous, compare);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createNot(Collection<T> innerInterfaces, CalcPropertyInterfaceImplement<T> implement) {
        List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(innerInterfaces.size());
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(innerInterfaces, listInterfaces);

        return new CalcPropertyMapImplement<JoinProperty.Interface, T>(new JoinProperty<PropertyInterface>(genID(), "sys",
                listInterfaces, false, NotFormulaProperty.instance.getImplement(implement.map(joinMap))), BaseUtils.reverse(joinMap));
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createNot(CalcPropertyInterfaceImplement<T> implement) {
        return createNot(getUsedInterfaces(implement), implement);
    }
    
    private static <T extends PropertyInterface> List<CalcPropertyInterfaceImplement<T>> transNot(List<CalcPropertyInterfaceImplement<T>> ands, List<Boolean> nots) {
        List<CalcPropertyInterfaceImplement<T>> result = new ArrayList<CalcPropertyInterfaceImplement<T>>();
        for(int i=0;i<nots.size();i++) {
            CalcPropertyInterfaceImplement<T> and = ands.get(i);
            if(nots.get(i))
                result.add(createNot(and));
            else
                result.add(and);
        }
        return result;
    }
    public static <P extends PropertyInterface> CalcPropertyMapImplement<?,P> createAnd(String name, List<P> listInterfaces, List<Boolean> nots) {
        return createAnd(name, "sys", listInterfaces, listInterfaces.get(0), BaseUtils.<List<CalcPropertyInterfaceImplement<P>>>immutableCast(listInterfaces.subList(1, listInterfaces.size())), nots);
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(String name, String caption, Collection<T> interfaces, CalcPropertyInterfaceImplement<T> object, List<CalcPropertyInterfaceImplement<T>> ands, List<Boolean> nots) {
        return createAnd(name, caption, interfaces, object, transNot(ands, nots));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(Collection<T> interfaces, CalcPropertyInterfaceImplement<T> object, List<CalcPropertyInterfaceImplement<T>> ands, List<Boolean> nots) {
        return createAnd(genID(), "sys", interfaces, object, ands, nots);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(Collection<T> interfaces, CalcPropertyInterfaceImplement<T> object, Collection<? extends CalcPropertyInterfaceImplement<T>> ands) {
        return createAnd(genID(), "sys", interfaces, object, ands);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(Collection<? extends CalcPropertyInterfaceImplement<T>> ands) {
        List<CalcPropertyInterfaceImplement<T>> rest = new ArrayList<CalcPropertyInterfaceImplement<T>>();
        return createAnd(getUsedInterfaces(ands), splitOne(ands, rest), rest);
    }
    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(String name, String caption, Collection<T> interfaces, CalcPropertyInterfaceImplement<T> object, Collection<? extends CalcPropertyInterfaceImplement<T>> ands) {
        if(ands.size()==0 && object instanceof CalcPropertyMapImplement)
            return (CalcPropertyMapImplement<?,T>)object;

        List<JoinProperty.Interface> listInterfaces = JoinProperty.getInterfaces(interfaces.size());
        Map<T, JoinProperty.Interface> joinMap = BaseUtils.buildMap(interfaces, listInterfaces);

        AndFormulaProperty implement = new AndFormulaProperty(genID(), ands.size());
        Map<AndFormulaProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = new HashMap<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>();
        joinImplement.put(implement.objectInterface,object.map(joinMap));
        Iterator<AndFormulaProperty.AndInterface> andIterator = implement.andInterfaces.iterator();
        for(CalcPropertyInterfaceImplement<T> and : ands)
            joinImplement.put(andIterator.next(),and.map(joinMap));

        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<AndFormulaProperty.Interface>(name,caption,
                listInterfaces,false,new CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(implement,joinImplement));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty,BaseUtils.reverse(joinMap));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAndNot(Collection<T> innerInterfaces, CalcPropertyInterfaceImplement<T> object, CalcPropertyInterfaceImplement<T> not) {
        return createAnd(genID(), "sys", innerInterfaces, object, Collections.singletonList(not), Collections.singletonList(true));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAndNot(CalcProperty<T> property, CalcPropertyInterfaceImplement<T> not) {
        return createAndNot(property.interfaces, property.getImplement(), not);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(CalcProperty<T> property, CalcPropertyInterfaceImplement<T> and) {
        return createAnd(property.interfaces, property.getImplement(), Collections.singleton(and));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(Collection<T> interfaces, CalcPropertyInterfaceImplement<T> object, CalcPropertyInterfaceImplement<T> and) {
        return createAnd(interfaces, object, Collections.singleton(and));
    }

    public static <MP extends PropertyInterface, MT extends PropertyInterface, P extends PropertyInterface, T extends PropertyInterface, C extends PropertyInterface> CalcPropertyMapImplement<?,C> createAnd(CalcPropertyMapImplement<T, C> object, CalcPropertyMapImplement<P, C> and) {
        return createAnd(getUsedInterfaces(toList(object, and)), object, and);
    }

    public static <P extends PropertyInterface, T extends PropertyInterface, C extends PropertyInterface> void createCommon(Collection<T> object, Collection<P> and, Map<T,P> map, Map<T, C> mapObject, Map<P, C> mapAnd) {
        Collection<C> commonInterfaces = new ArrayList<C>();
        for(P mapInterface : and) {
            C commonInterface = (C) new PropertyInterface();
            commonInterfaces.add(commonInterface);
            mapAnd.put(mapInterface, commonInterface);
        }
        for(T mapInterface : object) {
            C commonInterface = null;

            P mp = map.get(mapInterface);
            if(mp!=null)
                commonInterface = mapAnd.get(mp);

            if(commonInterface==null) {
                commonInterface = (C) new PropertyInterface();
                commonInterfaces.add(commonInterface);
            }

            mapObject.put(mapInterface, commonInterface);
        }
    }

    public static <T extends PropertyInterface, C extends PropertyInterface> CalcPropertyMapImplement<?,T> createUnion(Collection<T> interfaces, List<? extends CalcPropertyInterfaceImplement<T>> props) {
        List<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(interfaces.size());
        Map<T,UnionProperty.Interface> mapInterfaces = BaseUtils.buildMap(interfaces,listInterfaces);

        List<CalcPropertyInterfaceImplement<UnionProperty.Interface>> operands =
                DerivedProperty.mapCalcImplements(mapInterfaces, (List<CalcPropertyInterfaceImplement<T>>) props);
        OverrideUnionProperty unionProperty = new OverrideUnionProperty(genID(),"sys",listInterfaces,operands);
        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(unionProperty,BaseUtils.reverse(mapInterfaces));
    }

    public static <T extends PropertyInterface, C extends PropertyInterface> CalcPropertyMapImplement<?,T> createXUnion(Collection<T> interfaces, List<CalcPropertyInterfaceImplement<T>> props) {
        List<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(interfaces.size());
        Map<T,UnionProperty.Interface> mapInterfaces = BaseUtils.buildMap(interfaces,listInterfaces);

        List<CalcPropertyInterfaceImplement<UnionProperty.Interface>> operands = DerivedProperty.mapCalcImplements(mapInterfaces, props);
        ExclusiveUnionProperty unionProperty = new ExclusiveUnionProperty(genID(),"sys",listInterfaces,operands);
        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(unionProperty,BaseUtils.reverse(mapInterfaces));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createUnion(Collection<T> interfaces, CalcPropertyInterfaceImplement<T> first, CalcPropertyInterfaceImplement<T> rest) {
        return createUnion(interfaces, BaseUtils.toList(first, rest));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createXUnion(Collection<T> interfaces, CalcPropertyInterfaceImplement<T> first, CalcPropertyInterfaceImplement<T> rest) {
        return createXUnion(interfaces, BaseUtils.toList(first, rest));
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createDiff(CalcProperty<T> restriction, CalcPropertyInterfaceImplement<T> from) {
        List<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(restriction.interfaces.size());
        Map<T,UnionProperty.Interface> mapInterfaces = BaseUtils.buildMap(restriction.interfaces,listInterfaces);

        Map<CalcPropertyInterfaceImplement<UnionProperty.Interface>, Integer> operands = new HashMap<CalcPropertyInterfaceImplement<UnionProperty.Interface>, Integer>();
        operands.put(from.map(mapInterfaces),1);
        operands.put(new CalcPropertyMapImplement<T,UnionProperty.Interface>(restriction,mapInterfaces),-1);
        SumUnionProperty unionProperty = new SumUnionProperty(genID(),"sys",listInterfaces,operands);
        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(unionProperty,BaseUtils.reverse(mapInterfaces));
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createSum(String sID, String caption, Collection<T> interfaces, CalcPropertyInterfaceImplement<T> sum1, CalcPropertyInterfaceImplement<T> sum2) {
        List<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(interfaces.size());
        Map<T,UnionProperty.Interface> mapInterfaces = BaseUtils.buildMap(interfaces,listInterfaces);

        Map<CalcPropertyInterfaceImplement<UnionProperty.Interface>, Integer> operands = new HashMap<CalcPropertyInterfaceImplement<UnionProperty.Interface>, Integer>();
        operands.put(sum1.map(mapInterfaces),1);
        operands.put(sum2.map(mapInterfaces),1);
        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(new SumUnionProperty(sID,caption,listInterfaces,operands),BaseUtils.reverse(mapInterfaces));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, PropertyInterface> createLogical(boolean value) {
        return value ? createTrue() : createFalse();
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createTrue() {
        return createStatic(true, LogicalClass.instance);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createFalse() {
        return new CalcPropertyMapImplement<ClassPropertyInterface, T>(new SessionDataProperty(genID(), "sys", LogicalClass.instance));
//        return new CalcPropertyMapImplement<PropertyInterface, T>(NullValueProperty.instance, new HashMap<PropertyInterface, T>());
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createNotNull(CalcPropertyInterfaceImplement<T> notNull) {
        return createAnd(getUsedInterfaces(notNull), DerivedProperty.<T>createTrue(), notNull);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createStatic(Object value, StaticClass valueClass) {
        return new CalcPropertyMapImplement<PropertyInterface,T>(new ValueProperty(genID(),"sys", value, valueClass),new HashMap<PropertyInterface, T>());
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createStatic(String value) {
        return createStatic(value, StringClass.get(value.length()));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createNull() {
        return new CalcPropertyMapImplement<PropertyInterface, T>(NullValueProperty.instance);
    }

    public static <T extends PropertyInterface> CalcProperty createAnyGProp(CalcProperty<T> property) {
        return createAnyGProp(property, new ArrayList<T>()).property;
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createAnyGProp(CalcProperty<T> prop, Collection<T> groupInterfaces) {
        return createAnyGProp("ANY_" + prop.getSID()+"_"+BaseUtils.toString(groupInterfaces, "_"), "ANY " + prop.caption + " (" + BaseUtils.toString(groupInterfaces, ",") + ")", prop, groupInterfaces);
    }
    public static <T extends PropertyInterface, N extends PropertyInterface> CalcPropertyMapImplement<?, T> createAnyGProp(String sID, String caption, CalcProperty<T> prop, Collection<T> groupInterfaces) {
        if(!prop.getType().equals(LogicalClass.instance)) { // делаем Logical
            CalcPropertyMapImplement<N, T> notNull = (CalcPropertyMapImplement<N, T>) DerivedProperty.createNotNull(prop.getImplement());
            return DerivedProperty.<N, T>createAnyGProp(sID, caption, notNull.property, BaseUtils.filterValues(notNull.mapping, groupInterfaces).keySet()).map(notNull.mapping);
        }
        MaxGroupProperty<T> groupProperty = new MaxGroupProperty<T>(sID, caption, BaseUtils.<Collection<CalcPropertyInterfaceImplement<T>>>immutableCast(groupInterfaces), prop, false);
        return new CalcPropertyMapImplement<GroupProperty.Interface<T>, T>(groupProperty, BaseUtils.<Map<GroupProperty.Interface<T>, T>>immutableCast(groupProperty.getMapInterfaces()));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createLastGProp(CalcProperty<T> where, CalcPropertyInterfaceImplement<T> last, Collection<T> groupInterfaces, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        OrderGroupProperty<T> groupProperty = new OrderGroupProperty<T>(genID(), "sys", where.interfaces, BaseUtils.<Collection<CalcPropertyInterfaceImplement<T>>>immutableCast(groupInterfaces), toList(where.getImplement(), last), GroupType.LAST, orders, ordersNotNull);
        return new CalcPropertyMapImplement<GroupProperty.Interface<T>, T>(groupProperty, BaseUtils.<Map<GroupProperty.Interface<T>, T>>immutableCast(groupProperty.getMapInterfaces()));
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
        AndFormulaProperty andPrevious = new AndFormulaProperty(genID(), partProperties.size() + 1);
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
        Map<GroupProperty.Interface<JoinProperty.Interface>,JoinProperty.Interface> mapInterfaces = immutableCast(groupProperty.getMapInterfaces());
        return new CalcPropertyMapImplement<GroupProperty.Interface<JoinProperty.Interface>,T>(groupProperty,BaseUtils.join(mapInterfaces,BaseUtils.reverse(mapMain)));
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createOProp(CalcProperty<T> property, Collection<CalcPropertyInterfaceImplement<T>> partitions, OrderedMap<CalcPropertyInterfaceImplement<T>,Boolean> orders, boolean includeLast) {
        return createOProp(genID(), "sys", PartitionType.SUM, property, partitions, orders, includeLast);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createOProp(String sID, String caption, PartitionType partitionType, CalcProperty<T> property, Collection<CalcPropertyInterfaceImplement<T>> partitions, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean includeLast) {
        if(true) {
            List<CalcPropertyInterfaceImplement<T>> propList = Collections.<CalcPropertyInterfaceImplement<T>>singletonList(property.getImplement());
            return createOProp(sID, caption, partitionType, property.interfaces, propList, partitions, orders, Settings.instance.isDefaultOrdersNotNull(), includeLast);
        }

        assert orders.size()>0;
        
        if(orders.size()==1) return createSOProp(sID, caption, property, partitions, orders.singleKey(), orders.singleValue(), includeLast);
        // итеративно делаем Union, перекидывая order'ы в partition'ы
        List<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(property.interfaces.size());
        Map<T,UnionProperty.Interface> mapInterfaces = BaseUtils.buildMap(property.interfaces,listInterfaces);

        Map<CalcPropertyInterfaceImplement<UnionProperty.Interface>, Integer> operands = new HashMap<CalcPropertyInterfaceImplement<UnionProperty.Interface>, Integer>();
        Iterator<Map.Entry<CalcPropertyInterfaceImplement<T>,Boolean>> it = orders.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<CalcPropertyInterfaceImplement<T>,Boolean> order = it.next();
            operands.put(createSOProp(property, partitions, order.getKey(), order.getValue(), it.hasNext() && includeLast).map(mapInterfaces),1);
            partitions = new ArrayList<CalcPropertyInterfaceImplement<T>>(partitions);
            partitions.add(order.getKey());
        }

        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(new SumUnionProperty(sID,caption,listInterfaces,operands),BaseUtils.reverse(mapInterfaces));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createOProp(String sID, String caption, PartitionType partitionType, Collection<T> innerInterfaces, List<CalcPropertyInterfaceImplement<T>> props, Collection<CalcPropertyInterfaceImplement<T>> partitions, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull, boolean includeLast) {
        PartitionProperty<T> orderProperty = new PartitionProperty<T>(sID, caption, partitionType, innerInterfaces, props, partitions, orders, ordersNotNull, includeLast);
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
        return createUGProp(genID(), "sys", restriction.interfaces, group, orders, Settings.instance.isDefaultOrdersNotNull(), restriction.getImplement(), over);
    }
    public static <L extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<?,T> createUGProp(String sID, String caption, Collection<T> innerInterfaces, CalcPropertyImplement<L, CalcPropertyInterfaceImplement<T>> group, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull, CalcPropertyInterfaceImplement<T> restriction, boolean over) {
        Collection<CalcPropertyInterfaceImplement<T>> partitions = group.mapping.values();

        // строим связное distribute св-во, узнаем все использованные интерфейсы, строим map
        CalcPropertyMapImplement<?, T> distribute = createJoin(group);

        if(true) {
            PartitionProperty<T> orderProperty = new PartitionProperty<T>(sID, caption, over ? PartitionType.DISTR_RESTRICT_OVER : PartitionType.DISTR_RESTRICT, innerInterfaces, toList(restriction, distribute), partitions, orders, ordersNotNull, true);
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

    public static <L extends PropertyInterface, T extends PropertyInterface<T>> CalcPropertyMapImplement<?,T> createPGProp(String sID, String caption, int roundlen, boolean roundfirst, BaseClass baseClass, Collection<T> innerInterfaces, CalcPropertyImplement<L, CalcPropertyInterfaceImplement<T>> group, CalcPropertyInterfaceImplement<T> proportion, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {

        Collection<CalcPropertyInterfaceImplement<T>> partitions = group.mapping.values();

        // строим partition distribute св-во
        CalcPropertyMapImplement<?, T> distribute = createJoin(group);

        if(roundfirst && true) {
            PartitionProperty<T> orderProperty = new PartitionProperty<T>(sID, caption, PartitionType.DISTR_CUM_PROPORTION, innerInterfaces, toList(proportion, distribute), partitions, orders, ordersNotNull, false);
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

    public static <L extends PropertyInterface> CalcPropertyMapImplement<?, L> createIfElseUProp(Collection<L> innerInterfaces, CalcPropertyInterfaceImplement<L> ifProp, CalcPropertyMapImplement<?, L> trueProp, CalcPropertyMapImplement<?, L> falseProp, boolean ifClasses) {
        CalcPropertyMapImplement<?, L> ifTrue = null;
        if(trueProp!=null)
            ifTrue = DerivedProperty.createAnd(innerInterfaces, trueProp, ifProp);
        CalcPropertyMapImplement<?, L> ifFalse = null;
        if(falseProp!=null)
            ifFalse = DerivedProperty.createAndNot(innerInterfaces, falseProp, ifProp);
        
        if(ifTrue==null)
            return ifFalse;
        if(ifFalse==null)
            return ifTrue;
        return DerivedProperty.createXUnion(innerInterfaces, ifTrue, ifFalse);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<ClassPropertyInterface, T> createDataProp(boolean session, CalcProperty<T> property) {
        Map<T, ValueClass> interfaces = property.getInterfaceClasses();
        ValueClass valueClass = property.getValueClass();
        return createDataProp(session, "pc" + property.getSID(), property.toString(), interfaces, valueClass);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<ClassPropertyInterface, T> createDataProp(boolean session, Map<T, ValueClass> interfaces, ValueClass valueClass) {
        return createDataProp(session, genID(), "sys", interfaces, valueClass);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<ClassPropertyInterface, T> createDataProp(boolean session, String sID, String caption, Map<T, ValueClass> interfaces, ValueClass valueClass) {
        ValueClass[] interfaceClasses = new ValueClass[interfaces.size()];
        int iv = 0;
        List<T> listInterfaces = new ArrayList<T>();
        for(Map.Entry<T, ValueClass> entry : interfaces.entrySet()) {
            interfaceClasses[iv++] = entry.getValue();
            listInterfaces.add(entry.getKey());
        }
        DataProperty dataProperty = (session ? new SessionDataProperty(sID, caption, interfaceClasses, valueClass) :
                        new StoredDataProperty(sID, caption, interfaceClasses, valueClass));
        return dataProperty.getImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createIfAction(Collection<L> innerInterfaces, CalcPropertyMapImplement<?, L> where, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction, boolean ifClasses) {
        List<L> listInterfaces = new ArrayList<L>(innerInterfaces);
        IfActionProperty actionProperty = new IfActionProperty(genID(), "sys", false, listInterfaces, where, action, elseAction, ifClasses);
        return actionProperty.getImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createListAction(Collection<L> innerInterfaces, List<ActionPropertyMapImplement<?, L>> actions) {
        List<L> listInterfaces = new ArrayList<L>(innerInterfaces);
        ListActionProperty actionProperty = new ListActionProperty(genID(), "sys", listInterfaces, actions);
        return actionProperty.getImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(Collection<L> context, CalcPropertyMapImplement<?, L> forProp, OrderedMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction, boolean recursive) {
        return createForAction(context, forProp, orders, ordersNotNull, action, elseAction, null, null, false, recursive);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(Collection<L> innerInterfaces, Collection<L> context, CalcPropertyMapImplement<?, L> forProp, OrderedMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction, boolean recursive) {
        return createForAction(innerInterfaces, context, forProp, orders, ordersNotNull, action, elseAction, null, null, recursive);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(Collection<L> innerInterfaces, Collection<L> context, ActionPropertyMapImplement<?, L> action, L addObject, ConcreteCustomClass customClass, boolean recursive) {
        return createForAction(innerInterfaces, context, action, addObject, customClass, false, recursive);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(Collection<L> innerInterfaces, Collection<L> context, ActionPropertyMapImplement<?, L> action, L addObject, CustomClass customClass, boolean forceDialog, boolean recursive) {
        return createForAction(innerInterfaces, context, DerivedProperty.<L>createTrue(), new OrderedMap<CalcPropertyInterfaceImplement<L>, Boolean>(), false, action, null, addObject, customClass, forceDialog, recursive);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(Collection<L> innerInterfaces, Collection<L> context, CalcPropertyMapImplement<?, L> forProp, OrderedMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction, L addObject, ConcreteCustomClass customClass, boolean recursive) {
        return createForAction(innerInterfaces, context, forProp, orders, ordersNotNull, action, elseAction, addObject, customClass, false, recursive);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(Collection<L> context, CalcPropertyMapImplement<?, L> forProp, OrderedMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction, L addObject, CustomClass customClass, boolean forceDialog, boolean recursive) {
        Set<L> innerInterfaces = new HashSet<L>(context);
        innerInterfaces.addAll(forProp.mapping.values());
        innerInterfaces.addAll(action.mapping.values());
        if(elseAction != null)
            innerInterfaces.addAll(elseAction.mapping.values());
        if(addObject != null)
            innerInterfaces.add(addObject);
        innerInterfaces.addAll(getUsedInterfaces(orders.keySet()));

        return createForAction(innerInterfaces, new ArrayList<L>(context), forProp, orders, ordersNotNull, action, elseAction, addObject, customClass, forceDialog, recursive);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(Collection<L> innerInterfaces, Collection<L> context, CalcPropertyMapImplement<?, L> forProp, OrderedMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction, L addObject, CustomClass customClass, boolean forceDialog, boolean recursive) {
        return createForAction(innerInterfaces, new ArrayList<L>(context), forProp, orders, ordersNotNull, action, elseAction, addObject, customClass, forceDialog, recursive);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(Collection<L> innerInterfaces, List<L> mapInterfaces, CalcPropertyMapImplement<?, L> forProp, OrderedMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction, L addObject, CustomClass customClass, boolean forceDialog, boolean recursive) {
        ForActionProperty<L> actionProperty = new ForActionProperty<L>(genID(), "sys", innerInterfaces, mapInterfaces, forProp, orders, ordersNotNull, action, elseAction, addObject, customClass, forceDialog, recursive);
        return actionProperty.getMapImplement();
    }

    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createSetAction(Collection<L> context, CalcPropertyMapImplement<P, L> writeToProp, CalcPropertyInterfaceImplement<L> writeFrom) {
        return createSetAction(context, context, null, writeToProp, writeFrom);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createSetAction(Collection<L> innerInterfaces, Collection<L> context, CalcPropertyMapImplement<W, L> whereProp, CalcPropertyMapImplement<P, L> writeToProp, CalcPropertyInterfaceImplement<L> writeFrom) {
        return createSetAction(innerInterfaces, new ArrayList<L>(context), whereProp, writeToProp, writeFrom);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createSetAction(Collection<L> innerInterfaces, List<L> mapInterfaces, CalcPropertyMapImplement<W, L> whereProp, CalcPropertyMapImplement<P, L> writeToProp, CalcPropertyInterfaceImplement<L> writeFrom) {
        ChangeActionProperty<P, W, L> actionProperty = new ChangeActionProperty<P, W, L>(genID(), "sys", innerInterfaces, mapInterfaces, whereProp, writeToProp, writeFrom);
        return actionProperty.getMapImplement();
    }

    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createAddAction(CustomClass cls, boolean forceDialog, Collection<L> innerInterfaces, Collection<L> context, CalcPropertyMapImplement<W, L> whereProp, CalcPropertyMapImplement<P, L> resultProp) {
        return createAddAction(cls, forceDialog, innerInterfaces, new ArrayList<L>(context), whereProp, resultProp);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createAddAction(CustomClass cls, boolean forceDialog, Collection<L> innerInterfaces, List<L> mapInterfaces, CalcPropertyMapImplement<W, L> whereProp, CalcPropertyMapImplement<P, L> resultProp) {
        AddObjectActionProperty<W, L> actionProperty = new AddObjectActionProperty<W, L>(genID(), cls, forceDialog, innerInterfaces, mapInterfaces, whereProp, resultProp);
        return actionProperty.getMapImplement();
    }

    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createChangeClassAction(ObjectClass cls, boolean forceDialog, Collection<L> innerInterfaces, Collection<L> context, CalcPropertyMapImplement<W, L> whereProp, L changeInterface, BaseClass baseClass) {
        return createChangeClassAction(cls, forceDialog, innerInterfaces, new ArrayList<L>(context), whereProp, changeInterface, baseClass);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createChangeClassAction(ObjectClass cls, boolean forceDialog, Collection<L> innerInterfaces, List<L> mapInterfaces, CalcPropertyMapImplement<W, L> whereProp, L changeInterface, BaseClass baseClass) {
        ChangeClassActionProperty<W, L> actionProperty = new ChangeClassActionProperty<W, L>(genID(), cls, forceDialog, innerInterfaces, mapInterfaces, changeInterface, whereProp, baseClass);
        return actionProperty.getMapImplement();
    }

    // расширенный интерфейс создания SetAction, который умеет группировать, если что
//        FOR F(a,c,d,x) --- внеш. (e,x) + внутр. [a,c,d]
//            SET f(a,b) <- g(a,b,c,e)   --- внеш. (a,c,e) + внутр. [b]
//
//        SET f(a,b) <- [GROUP LAST F(a,c,d,x), g(a,b,c,e) ORDER O(a,c,d) BY a,b,c,e,x](a,b,c,e,x) WHERE [GROUP ANY F(a,c,d,x) BY a,c,x](a,c,x) --- внеш. (e,x) + внутр. [a,b,c]
    public static <W extends PropertyInterface, I extends PropertyInterface> ActionPropertyMapImplement<?, I> createSetAction(Collection<I> context, CalcPropertyMapImplement<?, I> writeTo, CalcPropertyInterfaceImplement<I> writeFrom, CalcPropertyMapImplement<W, I> where, OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull) {
        Collection<I> innerInterfaces = mergeColSet(writeTo.mapping.values(), context);
        Collection<I> whereInterfaces = where.mapping.values();
        assert merge(innerInterfaces, whereInterfaces).containsAll(getUsedInterfaces(writeFrom));

        if(!innerInterfaces.containsAll(whereInterfaces)) { // оптимизация, если есть допинтерфейсы - надо группировать
            if(!whereInterfaces.containsAll(getUsedInterfaces(writeFrom))) { // если не все ключи есть, придется докинуть or
                if(writeFrom instanceof CalcPropertyMapImplement)
                    where = (CalcPropertyMapImplement<W, I>) ChangeActionProperty.getFullProperty(mergeColSet(innerInterfaces, whereInterfaces), where, writeTo, writeFrom);
                else // по сути оптимизация, чтобы or не тянуть
                    where  = (CalcPropertyMapImplement<W, I>) DerivedProperty.createAnd(add(whereInterfaces, (I)writeFrom), where, ChangeActionProperty.getValueClassProperty(writeTo, writeFrom));
            }

            Map<W, I> mapPushInterfaces = filterValues(where.mapping, innerInterfaces); Map<I, W> mapWhere = reverse(where.mapping);
            writeFrom = createLastGProp(where.property, writeFrom.map(mapWhere), mapPushInterfaces.keySet(), mapImplements(orders, mapWhere), ordersNotNull).map(mapPushInterfaces);
            where = (CalcPropertyMapImplement<W, I>) createAnyGProp(where.property, mapPushInterfaces.keySet()).map(mapPushInterfaces);
        }

        return createSetAction(innerInterfaces, context, where, writeTo, writeFrom);
    }

    public static <W extends PropertyInterface, I extends PropertyInterface> ActionPropertyMapImplement<?, I> createChangeClassAction(Collection<I> context, I changeInterface, ObjectClass cls, boolean forceDialog, CalcPropertyMapImplement<W, I> where, BaseClass baseClass, OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull) {
        Collection<I> innerInterfaces = add(context, changeInterface);
        Collection<I> whereInterfaces = where.mapping.values();

        if(!innerInterfaces.containsAll(whereInterfaces)) { // оптимизация, если есть допинтерфейсы - надо группировать
            Map<W, I> mapPushInterfaces = filterValues(where.mapping, innerInterfaces);
            where = (CalcPropertyMapImplement<W, I>) createAnyGProp(where.property, mapPushInterfaces.keySet()).map(mapPushInterfaces);
        }

        return createChangeClassAction(cls, forceDialog, innerInterfaces, context, where, changeInterface, baseClass);
    }

}
