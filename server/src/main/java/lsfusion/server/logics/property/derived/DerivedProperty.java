package lsfusion.server.logics.property.derived;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;
import lsfusion.interop.Compare;
import lsfusion.server.Settings;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.expr.formula.CustomFormulaSyntax;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.AddObjectActionProperty;
import lsfusion.server.logics.property.actions.ChangeClassActionProperty;
import lsfusion.server.logics.property.actions.flow.*;
import lsfusion.server.logics.property.cases.ActionCase;
import lsfusion.server.logics.property.cases.CalcCase;

import java.util.List;

public class DerivedProperty {
    private static StaticClass formulaClass = DoubleClass.instance;

    // общие методы

    private static CalcPropertyImplement<CompareFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>> compareJoin(Compare compare, CalcPropertyInterfaceImplement<JoinProperty.Interface> operator1, CalcPropertyInterfaceImplement<JoinProperty.Interface> operator2) {
        CompareFormulaProperty compareProperty = new CompareFormulaProperty(compare);

        ImMap<CompareFormulaProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>> mapImplement =
                MapFact.<CompareFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>toMap(compareProperty.operator1, operator1, compareProperty.operator2, operator2);

        return new CalcPropertyImplement<CompareFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(compareProperty,mapImplement);
    }

    private static <P extends PropertyInterface, K extends PropertyInterface> GetValue<CalcPropertyInterfaceImplement<P>,CalcPropertyInterfaceImplement<K>> mapGetCalcValue(final ImRevMap<K, P> map) {
        return new GetValue<CalcPropertyInterfaceImplement<P>, CalcPropertyInterfaceImplement<K>>() {
            public CalcPropertyInterfaceImplement<P> getMapValue(CalcPropertyInterfaceImplement<K> value) {
                return value.map(map);
            }};
    }

    private static <P extends PropertyInterface, K extends PropertyInterface> GetValue<ActionPropertyMapImplement<?, P>,ActionPropertyMapImplement<?, K>> mapGetActionValue(final ImRevMap<K, P> map) {
        return new GetValue<ActionPropertyMapImplement<?, P>, ActionPropertyMapImplement<?, K>>() {
            public ActionPropertyMapImplement<?, P> getMapValue(ActionPropertyMapImplement<?, K> value) {
                return value.map(map);
            }};
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> ImCol<CalcPropertyInterfaceImplement<K>> mapImplements(ImCol<? extends CalcPropertyInterfaceImplement<T>> interfaceImplements, ImRevMap<T,K> map) {
        return ((ImCol<CalcPropertyInterfaceImplement<T>>)interfaceImplements).mapColValues(DerivedProperty.<K, T>mapGetCalcValue(map));
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> ImList<CalcPropertyInterfaceImplement<K>> mapImplements(ImList<? extends CalcPropertyInterfaceImplement<T>> interfaceImplements, ImRevMap<T,K> map) {
        return ((ImList<CalcPropertyInterfaceImplement<T>>)interfaceImplements).mapListValues(DerivedProperty.<K, T>mapGetCalcValue(map));
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> ImSet<CalcPropertyInterfaceImplement<K>> mapImplements(ImSet<? extends CalcPropertyInterfaceImplement<T>> interfaceImplements, ImRevMap<T,K> map) {
        return ((ImSet<CalcPropertyInterfaceImplement<T>>)interfaceImplements).mapSetValues(DerivedProperty.<K, T>mapGetCalcValue(map));
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> ImOrderMap<CalcPropertyInterfaceImplement<K>, Boolean> mapImplements(ImOrderMap<? extends CalcPropertyInterfaceImplement<T>, Boolean> interfaceImplements, ImRevMap<T,K> map) {
        return ((ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean>)interfaceImplements).mapOrderKeys(DerivedProperty.<K, T>mapGetCalcValue(map));
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface, C extends PropertyInterface> ImList<CalcPropertyInterfaceImplement<K>> mapCalcImplements(ImRevMap<T, K> map, ImList<CalcPropertyInterfaceImplement<T>> propertyImplements) {
        return mapImplements(propertyImplements, map);
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> ImList<ActionPropertyMapImplement<?, K>> mapActionImplements(ImRevMap<T,K> map, ImList<ActionPropertyMapImplement<?, T>> propertyImplements) {
        return propertyImplements.mapListValues(DerivedProperty.<K, T>mapGetActionValue(map));
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> ImMap<L,CalcPropertyInterfaceImplement<K>> mapImplements(ImMap<L,CalcPropertyInterfaceImplement<T>> interfaceImplements, ImRevMap<T,K> map) {
        return interfaceImplements.mapValues(DerivedProperty.<K, T>mapGetCalcValue(map));
    }

    public static <T extends PropertyInterface,K extends PropertyInterface, P extends PropertyInterface> ActionPropertyImplement<P, CalcPropertyInterfaceImplement<K>> mapActionImplements(ActionPropertyImplement<P, CalcPropertyInterfaceImplement<T>> implement, ImRevMap<T,K> map) {
        return new ActionPropertyImplement<P, CalcPropertyInterfaceImplement<K>>(implement.property,mapImplements(implement.mapping,map));
    }

    public static <T extends PropertyInterface> ImSet<T> getUsedInterfaces(CalcPropertyInterfaceImplement<T> interfaceImplement) {
        if(interfaceImplement instanceof CalcPropertyMapImplement)
            return ((CalcPropertyMapImplement<?, T>)interfaceImplement).mapping.valuesSet();
        else
            return SetFact.singleton((T) interfaceImplement);
    }

    public static <T extends PropertyInterface> ImSet<T> getUsedInterfaces(ImCol<? extends CalcPropertyInterfaceImplement<T>> col) {
        MSet<T> mUsedInterfaces = SetFact.mSet();
        for(CalcPropertyInterfaceImplement<T> interfaceImplement : col)
            mUsedInterfaces.addAll(getUsedInterfaces(interfaceImplement));
        return mUsedInterfaces.immutable();
    }

    // фильтрует только используемые интерфейсы и создает Join свойство с mapping'ом на эти интерфейсы
    public static <L extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<?,T> createJoin(CalcPropertyImplement<L, CalcPropertyInterfaceImplement<T>> implement) {
        // определяем какие интерфейсы использовали
        ImSet<T> usedInterfaces = getUsedInterfaces(implement.mapping.values());

        // создаем свойство - перемаппим интерфейсы
        ImRevMap<T,JoinProperty.Interface> joinMap = usedInterfaces.mapRevValues(JoinProperty.genInterface); // строим карту
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();
        JoinProperty<L> joinProperty = new JoinProperty<L>("sys", revJoinMap.keys().toOrderSet(),
                new CalcPropertyImplement<L, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(implement.property,mapImplements(implement.mapping,joinMap)));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty, revJoinMap);
    }

    public static <L extends PropertyInterface, T extends PropertyInterface> ActionPropertyMapImplement<?,T> createJoinAction(ActionPropertyImplement<L, CalcPropertyInterfaceImplement<T>> implement) {
        // определяем какие интерфейсы использовали
        ImSet<T> usedInterfaces = getUsedInterfaces(implement.mapping.values());

        // создаем свойство - перемаппим интерфейсы
        ImRevMap<T,PropertyInterface> joinMap = usedInterfaces.mapRevValues(Property.genInterface); // строим карту
        ImRevMap<PropertyInterface, T> revJoinMap = joinMap.reverse();
        JoinActionProperty<L> joinProperty = new JoinActionProperty<L>("sys", revJoinMap.keys().toOrderSet(),
                new ActionPropertyImplement<L, CalcPropertyInterfaceImplement<PropertyInterface>>(implement.property,mapImplements(implement.mapping,joinMap)));
        return new ActionPropertyMapImplement<PropertyInterface,T>(joinProperty, revJoinMap);
    }

    
    
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createCompare(Compare compare, T operator1, T operator2) {
        CompareFormulaProperty compareProperty = new CompareFormulaProperty(compare);

        ImRevMap<CompareFormulaProperty.Interface,T> mapImplement = MapFact.toRevMap(compareProperty.operator1, operator1, compareProperty.operator2, operator2);
        return new CalcPropertyMapImplement<CompareFormulaProperty.Interface, T>(compareProperty,mapImplement);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createCompare(String caption, ImSet<T> interfaces, CalcPropertyInterfaceImplement<T> distribute, CalcPropertyInterfaceImplement<T> previous, Compare compare) {
        ImRevMap<T, JoinProperty.Interface> joinMap = interfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();
        JoinProperty<CompareFormulaProperty.Interface> joinProperty = new JoinProperty<CompareFormulaProperty.Interface>(caption,
                revJoinMap.keys().toOrderSet(), compareJoin(compare, distribute.map(joinMap), previous.map(joinMap)));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty, revJoinMap);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createCompare(ImSet<T> interfaces, CalcPropertyInterfaceImplement<T> distribute, CalcPropertyInterfaceImplement<T> previous, Compare compare) {
        return createCompare("sys", interfaces, distribute, previous, compare);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createCompare(CalcPropertyInterfaceImplement<T> distribute, CalcPropertyInterfaceImplement<T> previous, Compare compare) {
        return createCompare(getUsedInterfaces(SetFact.toSet(distribute, previous)), distribute, previous, compare);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createNot(ImSet<T> innerInterfaces, CalcPropertyInterfaceImplement<T> implement) {
        ImRevMap<T, JoinProperty.Interface> joinMap = innerInterfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();

        return new CalcPropertyMapImplement<JoinProperty.Interface, T>(new JoinProperty<PropertyInterface>("sys",
                revJoinMap.keys().toOrderSet(), NotFormulaProperty.instance.getImplement(implement.map(joinMap))), revJoinMap);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createNot(CalcPropertyInterfaceImplement<T> implement) {
        return createNot(getUsedInterfaces(implement), implement);
    }
    
    private static <T extends PropertyInterface> ImList<CalcPropertyInterfaceImplement<T>> transNot(final ImList<CalcPropertyInterfaceImplement<T>> ands, final ImList<Boolean> nots) {
        return ands.mapListValues(new GetIndexValue<CalcPropertyInterfaceImplement<T>, CalcPropertyInterfaceImplement<T>>() {
            public CalcPropertyInterfaceImplement<T> getMapValue(int i, CalcPropertyInterfaceImplement<T> value) {
                if(nots.get(i))
                    return createNot(value);
                return value;
            }});
    }
    public static <P extends PropertyInterface> CalcPropertyMapImplement<?,P> createAnd(ImOrderSet<P> listInterfaces, ImList<Boolean> nots) {
        return createAnd("sys", listInterfaces.getSet(), listInterfaces.get(0), BaseUtils.<ImOrderSet<CalcPropertyInterfaceImplement<P>>>immutableCast(listInterfaces.subList(1, listInterfaces.size())), nots);
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(String caption, ImSet<T> interfaces, CalcPropertyInterfaceImplement<T> object, ImList<CalcPropertyInterfaceImplement<T>> ands, ImList<Boolean> nots) {
        return createAnd(caption, interfaces, object, transNot(ands, nots).getCol());
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(ImSet<T> interfaces, CalcPropertyInterfaceImplement<T> object, ImList<CalcPropertyInterfaceImplement<T>> ands, ImList<Boolean> nots) {
        return createAnd("sys", interfaces, object, ands, nots);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(ImSet<T> interfaces, CalcPropertyInterfaceImplement<T> object, ImCol<? extends CalcPropertyInterfaceImplement<T>> ands) {
        return createAnd("sys", interfaces, object, ands);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(ImCol<? extends CalcPropertyInterfaceImplement<T>> ands) {
        ImList<? extends CalcPropertyInterfaceImplement<T>> andsList = ands.toList();
        return createAnd(getUsedInterfaces(ands), andsList.get(0), andsList.subList(1, andsList.size()).getCol());
    }
    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(String caption, ImSet<T> interfaces, CalcPropertyInterfaceImplement<T> object, final ImCol<? extends CalcPropertyInterfaceImplement<T>> ands) {
        if(ands.size()==0 && object instanceof CalcPropertyMapImplement)
            return (CalcPropertyMapImplement<?,T>)object;

        final ImRevMap<T, JoinProperty.Interface> joinMap = interfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();

        AndFormulaProperty implement = new AndFormulaProperty(ands.size());
        ImMap<AndFormulaProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>> joinImplement =
                        MapFact.<AndFormulaProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>>addExcl(
                            implement.andInterfaces.mapValues(new GetIndex<CalcPropertyInterfaceImplement<JoinProperty.Interface>>() {
                            public CalcPropertyInterfaceImplement<JoinProperty.Interface> getMapValue(int i) {
                                return ands.get(i).map(joinMap);
                            }}), implement.objectInterface,object.map(joinMap));

        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<AndFormulaProperty.Interface>(caption,
                revJoinMap.keys().toOrderSet(),new CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(implement,joinImplement));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty, revJoinMap);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAndNot(ImSet<T> innerInterfaces, CalcPropertyInterfaceImplement<T> object, CalcPropertyInterfaceImplement<T> not) {
        return createAnd("sys", innerInterfaces, object, ListFact.singleton(not), ListFact.singleton(true));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAndNot(CalcProperty<T> property, CalcPropertyInterfaceImplement<T> not) {
        return createAndNot(property.interfaces, property.getImplement(), not);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(CalcProperty<T> property, CalcPropertyInterfaceImplement<T> and) {
        return createAnd(property.interfaces, property.getImplement(), SetFact.singleton(and));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createAnd(ImSet<T> interfaces, CalcPropertyInterfaceImplement<T> object, CalcPropertyInterfaceImplement<T> and) {
        return createAnd(interfaces, object, SetFact.singleton(and));
    }

    public static <MP extends PropertyInterface, MT extends PropertyInterface, P extends PropertyInterface, T extends PropertyInterface, C extends PropertyInterface> CalcPropertyMapImplement<?,C> createAnd(CalcPropertyMapImplement<T, C> object, CalcPropertyInterfaceImplement<C> and) {
        return createAnd(getUsedInterfaces(SetFact.toSet(object, and)), object, and);
    }

    public static <P extends PropertyInterface, T extends PropertyInterface, C extends PropertyInterface> void createCommon(ImSet<T> object, ImSet<P> and, final ImRevMap<T,P> map, Result<ImRevMap<T, C>> mapObject, final Result<ImRevMap<P, C>> mapAnd) {
        mapAnd.set(and.mapRevValues(new GetStaticValue<C>() {
            public C getMapValue() {
                return (C)new PropertyInterface();
            }}));
        mapObject.set(object.mapRevValues(new GetValue<C, T>() {
            public C getMapValue(T value) {
                P mp = map.get(value);
                if (mp != null)
                    return mapAnd.result.get(mp);
                return (C) new PropertyInterface();
            }
        }));
    }

    private static <T extends PropertyInterface,P extends PropertyInterface> ImOrderSet<JoinProperty.Interface> createSelfProp(ImSet<T> interfaces, Result<ImRevMap<T, JoinProperty.Interface>> map1, Result<ImRevMap<T, JoinProperty.Interface>> map2, Result<ImRevMap<T, JoinProperty.Interface>> mapCommon, ImSet<T> partInterfaces) {
        assert interfaces.containsAll(partInterfaces);
        int partSize = partInterfaces.size(); int intSize = interfaces.size();
        ImOrderSet<JoinProperty.Interface> result = SetFact.toOrderExclSet(2 * intSize - partSize, JoinProperty.genInterface);
        mapCommon.set(partInterfaces.toOrderSet().mapSet(result.subOrder(0, partSize)));
        ImOrderSet<T> diffInterfaces = interfaces.remove(partInterfaces).toOrderSet(); // Incl
        map1.set(diffInterfaces.mapSet(result.subOrder(partSize, intSize)));
        map2.set(diffInterfaces.mapSet(result.subOrder(intSize, 2 * intSize - partSize)));
        return result;
    }


    public static <T extends PropertyInterface, C extends PropertyInterface> CalcPropertyMapImplement<?,T> createUnion(ImSet<T> interfaces, ImList<? extends CalcPropertyInterfaceImplement<T>> props) {
        return createUnion(interfaces, props, false);
    }
    
    public static <T extends PropertyInterface, C extends PropertyInterface> CalcPropertyMapImplement<?,T> createUnion(ImSet<T> interfaces, ImList<? extends CalcPropertyInterfaceImplement<T>> props, boolean isExclusive) {
        ImRevMap<T,UnionProperty.Interface> mapInterfaces = interfaces.mapRevValues(UnionProperty.genInterface);
        ImRevMap<UnionProperty.Interface, T> revMapInterfaces = mapInterfaces.reverse();

        ImList<CalcPropertyInterfaceImplement<UnionProperty.Interface>> operands =
                DerivedProperty.mapCalcImplements(mapInterfaces, (ImList<CalcPropertyInterfaceImplement<T>>) props);
        CaseUnionProperty unionProperty = new CaseUnionProperty("sys", revMapInterfaces.keys().toOrderSet(), operands, false, isExclusive, true);
        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(unionProperty, revMapInterfaces);
    }

    public static <T extends PropertyInterface, C extends PropertyInterface> CalcPropertyMapImplement<UnionProperty.Interface,T> createUnion(boolean checkExclusive, boolean checkAll, boolean isLast, CaseUnionProperty.Type type, ImSet<T> interfaces, ValueClass valueClass, ImMap<T, ValueClass> interfaceClasses) {
        ImRevMap<T,UnionProperty.Interface> mapInterfaces = interfaces.mapRevValues(UnionProperty.genInterface);
        ImRevMap<UnionProperty.Interface, T> revMapInterfaces = mapInterfaces.reverse();
        CaseUnionProperty unionProperty = new CaseUnionProperty(checkExclusive, checkAll, isLast, type, "sys", revMapInterfaces.keys().toOrderSet(), valueClass, revMapInterfaces.join(interfaceClasses));
        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(unionProperty, revMapInterfaces);
    }

    public static <T extends PropertyInterface, C extends PropertyInterface> CalcPropertyMapImplement<?,T> createUnion(ImSet<T> interfaces, boolean isExclusive, ImList<CalcCase<T>> props) {
        final ImRevMap<T,UnionProperty.Interface> mapInterfaces = interfaces.mapRevValues(UnionProperty.genInterface);
        ImRevMap<UnionProperty.Interface, T> revMapInterfaces = mapInterfaces.reverse();

        ImList<CalcCase<UnionProperty.Interface>> cases = props.mapListValues(new GetValue<CalcCase<UnionProperty.Interface>, CalcCase<T>>() {
            public CalcCase<UnionProperty.Interface> getMapValue(CalcCase<T> value) {
                return value.map(mapInterfaces);
            }
        });
        CaseUnionProperty unionProperty = new CaseUnionProperty("sys", revMapInterfaces.keys().toOrderSet(), isExclusive, cases);
        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(unionProperty, revMapInterfaces);
    }

    public static <T extends PropertyInterface, C extends PropertyInterface> CalcPropertyMapImplement<?,T> createXUnion(ImSet<T> interfaces, ImList<CalcPropertyInterfaceImplement<T>> props) {
        ImRevMap<T,UnionProperty.Interface> mapInterfaces = interfaces.mapRevValues(UnionProperty.genInterface);
        ImRevMap<UnionProperty.Interface, T> revMapInterfaces = mapInterfaces.reverse();

        ImList<CalcPropertyInterfaceImplement<UnionProperty.Interface>> operands = DerivedProperty.mapCalcImplements(mapInterfaces, props);
        CaseUnionProperty unionProperty = new CaseUnionProperty("sys", revMapInterfaces.keys().toOrderSet(),operands.getCol(), false);
        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(unionProperty, revMapInterfaces);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createUnion(ImSet<T> interfaces, CalcPropertyInterfaceImplement<T> first, CalcPropertyInterfaceImplement<T> rest) {
        return createUnion(interfaces, ListFact.toList(first, rest));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createXUnion(ImSet<T> interfaces, CalcPropertyInterfaceImplement<T> first, CalcPropertyInterfaceImplement<T> rest) {
        return createXUnion(interfaces, ListFact.toList(first, rest));
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createDiff(CalcProperty<T> restriction, CalcPropertyInterfaceImplement<T> from) {
        ImRevMap<T,UnionProperty.Interface> mapInterfaces = restriction.interfaces.mapRevValues(UnionProperty.genInterface);
        ImRevMap<UnionProperty.Interface, T> revMapInterfaces = mapInterfaces.reverse();

        ImMap<CalcPropertyInterfaceImplement<UnionProperty.Interface>, Integer> operands = MapFact.toRevMap(from.map(mapInterfaces), 1, new CalcPropertyMapImplement<T, UnionProperty.Interface>(restriction, mapInterfaces), -1);
        SumUnionProperty unionProperty = new SumUnionProperty("sys", revMapInterfaces.keys().toOrderSet(),operands);
        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(unionProperty, revMapInterfaces);
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createSum(String caption, ImSet<T> interfaces, CalcPropertyInterfaceImplement<T> sum1, CalcPropertyInterfaceImplement<T> sum2) {
        ImRevMap<T,UnionProperty.Interface> mapInterfaces = interfaces.mapRevValues(UnionProperty.genInterface);
        ImRevMap<UnionProperty.Interface, T> revMapInterfaces = mapInterfaces.reverse();

        ImMap<CalcPropertyInterfaceImplement<UnionProperty.Interface>, Integer> operands = MapFact.toMap(sum1.map(mapInterfaces), 1, sum2.map(mapInterfaces), 1);
        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(new SumUnionProperty(caption, revMapInterfaces.keys().toOrderSet(),operands), revMapInterfaces);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, PropertyInterface> createLogical(boolean value) {
        return value ? createTrue() : createFalse();
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createTrue() {
        return createStatic(true, LogicalClass.instance);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createFalse() {
        return new CalcPropertyMapImplement<ClassPropertyInterface, T>(new SessionDataProperty("sys", LogicalClass.instance));
//        return new CalcPropertyMapImplement<PropertyInterface, T>(NullValueProperty.instance, new HashMap<PropertyInterface, T>());
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createNotNull(CalcPropertyInterfaceImplement<T> notNull) {
        return createAnd(getUsedInterfaces(notNull), DerivedProperty.<T>createTrue(), notNull);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createStatic(Object value, StaticClass valueClass) {
        return new CalcPropertyMapImplement<PropertyInterface,T>(new ValueProperty("sys", value, valueClass),MapFact.<PropertyInterface, T>EMPTYREV());
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createStatic(String value) {
        return createStatic(value, StringClass.get(value.length()));
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createNull() {
        return new CalcPropertyMapImplement<PropertyInterface, T>(NullValueProperty.instance);
    }

    public static <T extends PropertyInterface> CalcProperty createAnyGProp(CalcProperty<T> property) {
        return createAnyGProp(property, SetFact.<T>EMPTY()).property;
    }
    public static <T extends PropertyInterface, P extends PropertyInterface> CalcPropertyMapImplement<?, T> createAnyGProp(CalcPropertyMapImplement<P, T> implement, ImSet<T> groupInterfaces) {
        return createAnyGProp(implement.property, implement.mapping.filterInclValuesRev(groupInterfaces).keys()).map(implement.mapping);
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createAnyGProp(CalcProperty<T> prop, ImSet<T> groupInterfaces) {
        return createAnyGProp("ANY " + prop.caption + " (" + groupInterfaces.toString(",") + ")", prop, groupInterfaces);
    }
    public static <T extends PropertyInterface, N extends PropertyInterface> CalcPropertyMapImplement<?, T> createAnyGProp(String caption, CalcProperty<T> prop, ImSet<T> groupInterfaces) {
        if(!prop.getType().equals(LogicalClass.instance)) { // делаем Logical, может валиться по nullPointer, если в ACTION'е идут противоречивые условия (классы неправильные)
            CalcPropertyMapImplement<N, T> notNull = (CalcPropertyMapImplement<N, T>) DerivedProperty.createNotNull(prop.getImplement());
            return DerivedProperty.<N, T>createAnyGProp(caption, notNull.property, notNull.mapping.filterInclValuesRev(groupInterfaces).keys()).map(notNull.mapping);
        }
        MaxGroupProperty<T> groupProperty = new MaxGroupProperty<T>(caption, BaseUtils.<ImSet<CalcPropertyInterfaceImplement<T>>>immutableCast(groupInterfaces), prop, false);
        return new CalcPropertyMapImplement<GroupProperty.Interface<T>, T>(groupProperty, BaseUtils.<ImMap<GroupProperty.Interface<T>, T>>immutableCast(groupProperty.getMapInterfaces()).toRevExclMap());
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?, T> createLastGProp(CalcProperty<T> where, CalcPropertyInterfaceImplement<T> last, ImSet<T> groupInterfaces, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        OrderGroupProperty<T> groupProperty = new OrderGroupProperty<T>("sys", where.interfaces, BaseUtils.<ImCol<CalcPropertyInterfaceImplement<T>>>immutableCast(groupInterfaces), ListFact.toList(where.getImplement(), last), GroupType.LAST, orders, ordersNotNull);
        return new CalcPropertyMapImplement<GroupProperty.Interface<T>, T>(groupProperty, BaseUtils.<ImMap<GroupProperty.Interface<T>, T>>immutableCast(groupProperty.getMapInterfaces()).toRevExclMap());
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createFormula(ImSet<T> interfaces, String formula, DataClass valueClass, ImList<? extends CalcPropertyInterfaceImplement<T>> params) {
        return createFormula("sys", interfaces, formula, valueClass, params);
    }
    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createFormula(String caption, ImSet<T> interfaces, String formula, DataClass valueClass, ImList<? extends CalcPropertyInterfaceImplement<T>> params) {
        final ImRevMap<T, JoinProperty.Interface> joinMap = interfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();

        final StringFormulaProperty implement = new StringFormulaProperty(valueClass,new CustomFormulaSyntax(formula),params.size(), false);
        ImMap<StringFormulaProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = 
                ((ImList<CalcPropertyInterfaceImplement<T>>)params).mapListKeyValues(new GetIndex<StringFormulaProperty.Interface>() {
                    public StringFormulaProperty.Interface getMapValue(int i) {
                        return implement.findInterface("prm"+(i+1));
                    }}, mapGetCalcValue(joinMap));

        JoinProperty<StringFormulaProperty.Interface> joinProperty = new JoinProperty<StringFormulaProperty.Interface>(caption,
                revJoinMap.keys().toOrderSet(),new CalcPropertyImplement<StringFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(implement,joinImplement));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty, revJoinMap);
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createConcatenate(String caption, ImSet<T> interfaces, ImList<? extends CalcPropertyInterfaceImplement<T>> params) {
        ImRevMap<T, JoinProperty.Interface> joinMap = interfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();

        final ConcatenateProperty implement = new ConcatenateProperty(params.size());
        ImMap<ConcatenateProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>> joinImplement =
                ((ImList<CalcPropertyInterfaceImplement<T>>)params).mapListKeyValues(new GetIndex<ConcatenateProperty.Interface>() {
                            public ConcatenateProperty.Interface getMapValue(int i) {
                                return implement.getInterface(i);
                            }}, mapGetCalcValue(joinMap));

        JoinProperty<ConcatenateProperty.Interface> joinProperty = new JoinProperty<ConcatenateProperty.Interface>(caption,
                revJoinMap.keys().toOrderSet(), new CalcPropertyImplement<ConcatenateProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(implement,joinImplement));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty, revJoinMap);
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createDeconcatenate(String caption, CalcProperty<T> property, int part, BaseClass baseClass) {
        ImRevMap<T, JoinProperty.Interface> joinMap = property.interfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();

        DeconcatenateProperty implement = new DeconcatenateProperty(part,baseClass);
        ImMap<DeconcatenateProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = MapFact.<DeconcatenateProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>>singleton(implement.interfaces.single(), new CalcPropertyMapImplement<T, JoinProperty.Interface>(property, joinMap));

        JoinProperty<DeconcatenateProperty.Interface> joinProperty = new JoinProperty<DeconcatenateProperty.Interface>(caption,revJoinMap.keys().toOrderSet(),
                new CalcPropertyImplement<DeconcatenateProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(implement,joinImplement));
        return new CalcPropertyMapImplement<JoinProperty.Interface,T>(joinProperty, revJoinMap);
    }

    // PARTITION методы

    private static <P extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<?,JoinProperty.Interface> createCompareProp(CalcPropertyMapImplement<P, T> implement, ImRevMap<T, JoinProperty.Interface> map1, ImRevMap<T, JoinProperty.Interface> map2, ImRevMap<T, JoinProperty.Interface> mapCommon, Compare compare) {
        Result<ImRevMap<P,JoinProperty.Interface>> join1 = new Result<ImRevMap<P, JoinProperty.Interface>>(); Result<ImRevMap<P,JoinProperty.Interface>> join2 = new Result<ImRevMap<P, JoinProperty.Interface>>(); Result<ImRevMap<P, JoinProperty.Interface>> joinCommon = new Result<ImRevMap<P, JoinProperty.Interface>>();
        ImOrderSet<JoinProperty.Interface> listInterfaces = createSelfProp(implement.property.interfaces, join1, join2, joinCommon, implement.mapping.filterValuesRev(mapCommon.keys()).keys());
        JoinProperty<CompareFormulaProperty.Interface> joinProperty = new JoinProperty<CompareFormulaProperty.Interface>("sys",listInterfaces,
                compareJoin(compare, new CalcPropertyMapImplement<P, JoinProperty.Interface>(implement.property, join1.result.addRevExcl(joinCommon.result)), new CalcPropertyMapImplement<P, JoinProperty.Interface>(implement.property, join2.result.addRevExcl(joinCommon.result))));
        
        return new CalcPropertyMapImplement<JoinProperty.Interface,JoinProperty.Interface>(joinProperty,
                join1.result.crossJoin(implement.mapping.join(map1)).addRevExcl(
                join2.result.crossJoin(implement.mapping.join(map2))).addRevExcl(
                joinCommon.result.crossJoin(implement.mapping.join(mapCommon))));
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> CalcPropertyInterfaceImplement<JoinProperty.Interface> createInterfaceCompareMap(T implement, ImRevMap<T, JoinProperty.Interface> map1, ImRevMap<T, JoinProperty.Interface> map2, ImRevMap<T, JoinProperty.Interface> mapCommon, Compare compare) {
        JoinProperty.Interface commonInterface = mapCommon.get(implement);
        if(commonInterface!=null)
            return commonInterface;

        CompareFormulaProperty compareProperty = new CompareFormulaProperty(compare);
        ImRevMap<CompareFormulaProperty.Interface,JoinProperty.Interface> mapImplement = MapFact.toRevMap(compareProperty.operator1, map1.get(implement), compareProperty.operator2, map2.get(implement));
        return new CalcPropertyMapImplement<CompareFormulaProperty.Interface,JoinProperty.Interface>(compareProperty,mapImplement);
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> CalcPropertyInterfaceImplement<JoinProperty.Interface> createCompareMap(CalcPropertyInterfaceImplement<T> implement, ImRevMap<T, JoinProperty.Interface> map1, ImRevMap<T, JoinProperty.Interface> map2, ImRevMap<T, JoinProperty.Interface> mapCommon, Compare compare) {
        if(implement instanceof CalcPropertyMapImplement)
            return createCompareProp((CalcPropertyMapImplement<?,T>)implement, map1, map2, mapCommon, compare);
        else
            return createInterfaceCompareMap((T)implement, map1, map2, mapCommon, compare);
    }

    public static <T extends PropertyInterface> JoinProperty<AndFormulaProperty.Interface> createPartition(ImSet<T> interfaces, CalcPropertyInterfaceImplement<T> property, ImCol<CalcPropertyInterfaceImplement<T>> partitions, final CalcPropertyInterfaceImplement<T> expr, final Result<ImRevMap<T, JoinProperty.Interface>> mapMain, final Compare compare) {
        // "двоим" интерфейсы (для partition'а не PropertyInterface), для результ. св-ва
        MSet<T> mPartInterfaces = SetFact.mSet();
        MSet<CalcPropertyMapImplement<?,T>> mPartProperties = SetFact.mSet();
        for(CalcPropertyInterfaceImplement<T> partition : partitions)
            partition.fill(mPartInterfaces, mPartProperties);
        final ImSet<T> partInterfaces = mPartInterfaces.immutable();
        assert interfaces.containsAll(partInterfaces);
        final ImSet<CalcPropertyMapImplement<?, T>> partProperties = mPartProperties.immutable();

        final Result<ImRevMap<T,JoinProperty.Interface>> map1 = new Result<ImRevMap<T, JoinProperty.Interface>>();
        final Result<ImRevMap<T,JoinProperty.Interface>> map2 = new Result<ImRevMap<T, JoinProperty.Interface>>();
        final Result<ImRevMap<T,JoinProperty.Interface>> mapCommon = new Result<ImRevMap<T, JoinProperty.Interface>>();
        ImOrderSet<JoinProperty.Interface> listInterfaces = createSelfProp(interfaces, map1, map2, mapCommon, partInterfaces);

        // ставим equals'ы на partitions свойства (раздвоенные), greater на предшествие order (раздвоенное)
        AndFormulaProperty andPrevious = new AndFormulaProperty(partProperties.size() + 1);
        ImMap<AndFormulaProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>> mapImplement =
                MapFact.<AndFormulaProperty.Interface,CalcPropertyInterfaceImplement<JoinProperty.Interface>>addExcl(
                        andPrevious.andInterfaces.mapValues(new GetIndex<CalcPropertyInterfaceImplement<JoinProperty.Interface>>() {
                            public CalcPropertyInterfaceImplement<JoinProperty.Interface> getMapValue(int i) {
                                return i == 0 ? createCompareMap(expr, map1.result, map2.result, mapCommon.result, compare) : createCompareProp(partProperties.get(i-1), map1.result, map2.result, mapCommon.result, Compare.EQUALS);
                            }
                        }), andPrevious.objectInterface, property.map(map2.result.addRevExcl(mapCommon.result)));
        mapMain.set(map1.result.addRevExcl(mapCommon.result));

        return new JoinProperty<AndFormulaProperty.Interface>("sys",listInterfaces,
                new CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<JoinProperty.Interface>>(andPrevious, mapImplement));
    }

    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createOProp(CalcProperty<T> property, ImSet<CalcPropertyInterfaceImplement<T>> partitions, ImOrderMap<CalcPropertyInterfaceImplement<T>,Boolean> orders, boolean includeLast) {
        return createOProp("sys", PartitionType.SUM, property, partitions, orders, includeLast);
    }
    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createOProp(String caption, PartitionType partitionType, CalcProperty<T> property, ImSet<CalcPropertyInterfaceImplement<T>> partitions, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean includeLast) {
        if(true) {
            ImList<CalcPropertyInterfaceImplement<T>> propList = ListFact.<CalcPropertyInterfaceImplement<T>>singleton(property.getImplement());
            return createOProp(caption, partitionType, property.interfaces, propList, partitions, orders, Settings.get().isDefaultOrdersNotNull(), includeLast);
        }

        throw new UnsupportedOperationException();
 /*       assert orders.size()>0;
        
        if(orders.size()==1) return createSOProp(sID, caption, property, partitions, orders.singleKey(), orders.singleValue(), includeLast);
        // итеративно делаем Union, перекидывая order'ы в partition'ы
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(property.interfaces.size());
        ImRevMap<T,UnionProperty.Interface> mapInterfaces = property.interfaces.toRevMap(listInterfaces);

        ImMap<CalcPropertyInterfaceImplement<UnionProperty.Interface>, Integer> operands = new HashMap<CalcPropertyInterfaceImplement<UnionProperty.Interface>, Integer>();
        Iterator<ImMap.Entry<CalcPropertyInterfaceImplement<T>,Boolean>> it = orders.entrySet().iterator();
        while(it.hasNext()) {
            ImMap.Entry<CalcPropertyInterfaceImplement<T>,Boolean> order = it.next();
            operands.put(createSOProp(property, partitions, order.getKey(), order.getValue(), it.hasNext() && includeLast).map(mapInterfaces),1);
            partitions = new ArrayList<CalcPropertyInterfaceImplement<T>>(partitions);
            partitions.add(order.getKey());
        }

        return new CalcPropertyMapImplement<UnionProperty.Interface,T>(new SumUnionProperty(sID,caption,listInterfaces,operands),BaseUtils.reverse(mapInterfaces));*/
    }

    public static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createOProp(String caption, PartitionType partitionType, ImSet<T> innerInterfaces, ImList<CalcPropertyInterfaceImplement<T>> props, ImSet<CalcPropertyInterfaceImplement<T>> partitions, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull, boolean includeLast) {
        PartitionProperty<T> orderProperty = new PartitionProperty<T>(caption, partitionType, innerInterfaces, props, partitions, orders, ordersNotNull, includeLast);
        return new CalcPropertyMapImplement<PartitionProperty.Interface<T>,T>(orderProperty,orderProperty.getMapInterfaces());
    }

    public static <T> CalcPropertyRevImplement<?,T> createCProp(StaticClass valueClass, Object value, ImMap<T, ValueClass> params) {
        return createCProp("sys", valueClass, value, params);
    }

    public static <T> CalcPropertyRevImplement<?,T> createCProp(String caption, StaticClass valueClass, Object value, ImMap<T, ValueClass> params) {
        return createCProp(caption, valueClass instanceof LogicalClass && params.size() > 0 ? null : new ValueProperty("sys", value, valueClass), params);
    }

    public static <T> CalcPropertyRevImplement<?,T> createCProp(String caption, DataClass valueClass, ImMap<T, ValueClass> params) {
        return createCProp(caption, new InfiniteProperty("sys", valueClass), params);
    }

    public static <T,V extends PropertyInterface> CalcPropertyRevImplement<?,T> createCProp(String caption, CalcProperty<V> valueProperty, ImMap<T, ValueClass> params) {
        if(params.size()==0)
            return new CalcPropertyRevImplement<V, T>(valueProperty, MapFact.<V, T>EMPTYREV());

        ImRevMap<PropertyInterface, T> mapInterfaces = params.keys().mapRevKeys(new GetValue<PropertyInterface, T>() {
            public PropertyInterface getMapValue(T value) {
                return new PropertyInterface();
            }});
        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listImplements = mapInterfaces.join(params).mapColValues(new GetKeyValue<CalcPropertyInterfaceImplement<PropertyInterface>, PropertyInterface, ValueClass>() {
            public CalcPropertyInterfaceImplement<PropertyInterface> getMapValue(PropertyInterface key, ValueClass value) {
                return IsClassProperty.getProperty(value, "value").mapPropertyImplement(MapFact.singletonRev("value", key));
            }}).toList();

        CalcPropertyMapImplement<?, PropertyInterface> and;
        if(valueProperty==null)
            and = createAnd(caption, mapInterfaces.keys(), listImplements.get(0), listImplements.subList(1, listImplements.size()).getCol());
        else
            and = createAnd(caption, mapInterfaces.keys(), new CalcPropertyMapImplement<V, PropertyInterface>(valueProperty), listImplements.getCol());
        return and.mapRevImplement(mapInterfaces);
    }

    
    // строится partition с пустым order'ом
    private static <T extends PropertyInterface> CalcPropertyMapImplement<?,T> createPProp(ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerInterfaces, CalcPropertyInterfaceImplement<T> property, ImSet<CalcPropertyInterfaceImplement<T>> partitions, GroupType type) {
        GroupProperty<T> partitionGroup = type.createProperty("sys", innerInterfaces.getSet(), property, partitions);
        partitionGroup.setExplicitInnerClasses(innerInterfaces, explicitInnerInterfaces);
        return createJoin(new CalcPropertyImplement<GroupProperty.Interface<T>, CalcPropertyInterfaceImplement<T>>(partitionGroup, partitionGroup.getMapInterfaces()));
    }

    public static <L extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<?,T> createUGProp(CalcPropertyImplement<L, CalcPropertyInterfaceImplement<T>> group, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, CalcProperty<T> restriction, boolean over) {
        return createUGProp("sys", restriction.interfaces, group, orders, Settings.get().isDefaultOrdersNotNull(), restriction.getImplement(), over);
    }
    public static <L extends PropertyInterface, T extends PropertyInterface> CalcPropertyMapImplement<?,T> createUGProp(String caption, ImSet<T> innerInterfaces, CalcPropertyImplement<L, CalcPropertyInterfaceImplement<T>> group, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull, CalcPropertyInterfaceImplement<T> restriction, boolean over) {
        ImSet<CalcPropertyInterfaceImplement<T>> partitions = group.mapping.values().toSet();

        // строим связное distribute св-во, узнаем все использованные интерфейсы, строим map
        CalcPropertyMapImplement<?, T> distribute = createJoin(group);

        if(true) {
            PartitionProperty<T> orderProperty = new PartitionProperty<T>(caption, over ? PartitionType.DISTR_RESTRICT_OVER : PartitionType.DISTR_RESTRICT, innerInterfaces, ListFact.toList(restriction, distribute), partitions, orders, ordersNotNull, true);
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
        CalcPropertyMapImplement<?, T> min = createCustomFormula(restriction.interfaces, "(prm1+prm2-prm3-ABS(prm1-(prm2-prm3)))/2", formulaClass, BaseUtils.toList(restImplement, distribute, previous));

        // распр. > пред.
        CalcPropertyMapImplement<?, T> compare = createCompare(restriction.interfaces, distribute, previous, Compare.GREATER);

        // MIN2(огр., распр. - пред.) И распр. > пред.
        return createAnd(restriction.interfaces, min, compare);*/
    }

    public static <L extends PropertyInterface, T extends PropertyInterface<T>> CalcPropertyMapImplement<?,T> createPGProp(String caption, int roundlen, boolean roundfirst, BaseClass baseClass, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerInterfaces, CalcPropertyImplement<L, CalcPropertyInterfaceImplement<T>> group, CalcPropertyInterfaceImplement<T> proportion, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {

        ImSet<CalcPropertyInterfaceImplement<T>> partitions = group.mapping.values().toSet();

        // строим partition distribute св-во
        CalcPropertyMapImplement<?, T> distribute = createJoin(group);

        if(roundfirst && true) {
            PartitionProperty<T> orderProperty = new PartitionProperty<T>(caption, PartitionType.DISTR_CUM_PROPORTION, innerInterfaces.getSet(), ListFact.toList(proportion, distribute), partitions, orders, ordersNotNull, false);
            return new CalcPropertyMapImplement<PartitionProperty.Interface<T>, T>(orderProperty, orderProperty.getMapInterfaces());
        }

        // общая сумма по пропорции в partition'е
        CalcPropertyMapImplement<?, T> propSum = createPProp(innerInterfaces, explicitInnerInterfaces, proportion, partitions, GroupType.SUM);

        String distrCaption = !roundfirst ? caption : "sys";
        // округляем

        int numericLength = 15 + roundlen;
        CalcPropertyMapImplement<?, T> distrRound =
                createFormula(distrCaption, innerInterfaces.getSet(),
                              "ROUND(CAST((prm1*prm2/prm3) as NUMERIC(" + numericLength + "," + roundlen+ "))," + roundlen + ")", NumericClass.get(numericLength, roundlen), ListFact.toList(distribute, proportion, propSum));

        if (!roundfirst) return distrRound;
        
        throw new RuntimeException("not supported");

/*        // строим partition полученного округления
        CalcPropertyMapImplement<?, T> totRound = createPProp(distrRound, group.mapping.values(), GroupType.SUM);

        // получаем сколько надо дораспределить
        CalcPropertyMapImplement<?, T> diffRound = createCustomFormula(proportion.interfaces, "prm1-prm2", formulaClass, BaseUtils.toList(distribute, totRound)); // вообще гря разные параметры поэтому formula, а не diff

        // берем первую пропорцию 
        CalcPropertyMapImplement<?, T> proportionFirst = createFOProp(proportion, baseClass, group.mapping.values());

        // включаем всю оставшуюся сумму на нее
        CalcPropertyMapImplement<?, T> diffFirst = createAnd(proportion.interfaces, diffRound, proportionFirst);

        // делаем union с основным distrRound'ом
        return createSum(sID, caption, proportion.interfaces, distrRound, diffFirst);*/
    }

    private static <T extends PropertyInterface> CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>> createMaxProp(String caption, ImOrderSet<T> interfaces, List<ResolveClassSet> explicitInnerClasses, CalcPropertyInterfaceImplement<T> implement, ImCol<CalcPropertyInterfaceImplement<T>> group, boolean min) {
        MaxGroupProperty<T> maxProperty = new MaxGroupProperty<T>(caption, interfaces.getSet(), group, implement, min);
        maxProperty.setExplicitInnerClasses(interfaces, explicitInnerClasses);        
        return new CalcPropertyImplement<GroupProperty.Interface<T>, CalcPropertyInterfaceImplement<T>>(maxProperty, maxProperty.getMapInterfaces());
    }
    
    public static <T extends PropertyInterface> ImList<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> createMGProp(String[] captions, ImOrderSet<T> interfaces, List<ResolveClassSet> explicitInnerClasses, BaseClass baseClass, ImList<CalcPropertyInterfaceImplement<T>> props, ImCol<CalcPropertyInterfaceImplement<T>> group, MSet<CalcProperty> persist, boolean min) {
        if(props.size()==1)
            return createEqualsMGProp(captions, interfaces, explicitInnerClasses, props, group, persist, min);
        else
            return createConcMGProp(captions, interfaces, explicitInnerClasses, baseClass, props, group, persist, min);
    }

    private static <T extends PropertyInterface> ImList<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> createEqualsMGProp(String[] captions, ImOrderSet<T> interfaces, List<ResolveClassSet> explicitInnerClasses, ImList<CalcPropertyInterfaceImplement<T>> props, ImCol<CalcPropertyInterfaceImplement<T>> group, MSet<CalcProperty> persist, boolean min) {
        CalcPropertyInterfaceImplement<T> propertyImplement = props.get(0);

        MList<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> mResult = ListFact.mList();
        int i = 1;
        do {
            CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>> maxImplement = createMaxProp(captions[i-1], interfaces, explicitInnerClasses, propertyImplement, group, min);
            mResult.add(maxImplement);
            if(i<props.size()) { // если не последняя
                CalcPropertyMapImplement<?,T> prevMax = createJoin(maxImplement); // какой максимум в partition'е

                CalcPropertyMapImplement<?,T> equalsMax = createCompare(interfaces.getSet(), propertyImplement, prevMax, Compare.EQUALS);

                propertyImplement = createAnd(interfaces.getSet(), props.get(i), equalsMax);
            }
        } while (i++<props.size());
        return mResult.immutableList();
    }

    private static <T extends PropertyInterface,L extends PropertyInterface> ImList<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> createDeconcatenate(String[] captions, CalcPropertyImplement<L, CalcPropertyInterfaceImplement<T>> implement, int parts, BaseClass baseClass) {
        MList<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> mResult = ListFact.mList(parts);
        for(int i=0;i<parts;i++)
            mResult.add(createDeconcatenate(captions[i], implement.property, i, baseClass).mapImplement(implement.mapping));
        return mResult.immutableList();
    }

    private static <T extends PropertyInterface> ImList<CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>>> createConcMGProp(String[] captions, ImOrderSet<T> interfaces, List<ResolveClassSet> explicitInnerClasses, BaseClass baseClass, ImList<CalcPropertyInterfaceImplement<T>> props, ImCol<CalcPropertyInterfaceImplement<T>> group, MSet<CalcProperty> persist, boolean min) {
        String concCaption = BaseUtils.toString(", ", captions);

        CalcPropertyMapImplement<?, T> concate = createConcatenate("Concatenate - "+ concCaption, interfaces.getSet(), props);
        persist.add(concate.property);
        
        CalcPropertyImplement<?, CalcPropertyInterfaceImplement<T>> max = createMaxProp("Concatenate - " + concCaption, interfaces, explicitInnerClasses, concate, group, min);
        persist.add(max.property);

        return createDeconcatenate(captions, max, props.size(), baseClass);
    }

    public static <L extends PropertyInterface> CalcPropertyMapImplement<?, L> createIfElseUProp(ImSet<L> innerInterfaces, CalcPropertyInterfaceImplement<L> ifProp, CalcPropertyMapImplement<?, L> trueProp, CalcPropertyMapImplement<?, L> falseProp) {
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

    public static <T extends PropertyInterface> CalcPropertyMapImplement<ClassPropertyInterface, T> createForDataProp(ImMap<T, ValueClass> interfaces, ValueClass valueClass, MSet<SessionDataProperty> mLocals) {
        ImOrderMap<T, ValueClass> orderInterfaces = interfaces.toOrderMap();
        ImOrderSet<T> listInterfaces = orderInterfaces.keyOrderSet();
        ValueClass[] interfaceClasses = orderInterfaces.valuesList().toArray(new ValueClass[orderInterfaces.size()]);
        SessionDataProperty dataProperty = new SessionDataProperty("sys", interfaceClasses, valueClass, true);
        mLocals.add(dataProperty);
        return dataProperty.getImplement(listInterfaces);
    }

    public static <T> CalcPropertyRevImplement<ClassPropertyInterface, T> createDataPropRev(String caption, ImMap<T, ValueClass> interfaces, ValueClass valueClass) {
        ImOrderMap<T, ValueClass> orderInterfaces = interfaces.toOrderMap();
        ImOrderSet<T> listInterfaces = orderInterfaces.keyOrderSet();
        ValueClass[] interfaceClasses = orderInterfaces.valuesList().toArray(new ValueClass[orderInterfaces.size()]);
        DataProperty dataProperty = new SessionDataProperty(caption, interfaceClasses, valueClass);
        return dataProperty.getRevImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createIfAction(ImSet<L> innerInterfaces, CalcPropertyMapImplement<?, L> where, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction) {
        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        ActionProperty actionProperty = CaseActionProperty.createIf("sys", false, listInterfaces, where, action, elseAction);
        return actionProperty.getImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createCaseAction(ImSet<L> innerInterfaces, boolean isExclusive, ImList<ActionCase<L>> cases) {
        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        ActionProperty actionProperty = new CaseActionProperty("sys", false, listInterfaces, cases);
        return actionProperty.getImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createListAction(ImSet<L> innerInterfaces, ImList<ActionPropertyMapImplement<?, L>> actions) {
        return createListAction(innerInterfaces, actions, SetFact.<SessionDataProperty>EMPTY());
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createListAction(ImSet<L> innerInterfaces, ImList<ActionPropertyMapImplement<?, L>> actions, ImSet<SessionDataProperty> localsInScope) {
        if(actions.size()==1)
            return actions.single();

        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        ListActionProperty actionProperty = new ListActionProperty("sys", listInterfaces, actions, localsInScope);
        return actionProperty.getImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(ImSet<L> context, CalcPropertyMapImplement<?, L> forProp, ImOrderMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction, boolean recursive, ImSet<L> noInline, boolean forceInline) {
        return createForAction(context, forProp, orders, ordersNotNull, action, elseAction, null, null, false, recursive, noInline, forceInline);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(ImSet<L> innerInterfaces, ImSet<L> context, CalcPropertyMapImplement<?, L> forProp, ImOrderMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction, boolean recursive, ImSet<L> noInline, boolean forceInline) {
        return createForAction(innerInterfaces, context, forProp, orders, ordersNotNull, action, elseAction, null, null, recursive, noInline, forceInline);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(ImSet<L> innerInterfaces, ImSet<L> context, ActionPropertyMapImplement<?, L> action, L addObject, ConcreteCustomClass customClass, boolean recursive) {
        return createForAction(innerInterfaces, context, action, addObject, customClass, false, recursive, SetFact.<L>EMPTY(), false);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(ImSet<L> innerInterfaces, ImSet<L> context, ActionPropertyMapImplement<?, L> action, L addObject, CustomClass customClass, boolean forceDialog, boolean recursive, ImSet<L> noInline, boolean forceInline) {
        return createForAction(innerInterfaces, context, null, MapFact.<CalcPropertyInterfaceImplement<L>, Boolean>EMPTYORDER(), false, action, null, addObject, customClass, forceDialog, recursive, noInline, forceInline);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(ImSet<L> innerInterfaces, ImSet<L> context, CalcPropertyMapImplement<?, L> forProp, ImOrderMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction, L addObject, ConcreteCustomClass customClass, boolean recursive, ImSet<L> noInline, boolean forceInline) {
        return createForAction(innerInterfaces, context, forProp, orders, ordersNotNull, action, elseAction, addObject, customClass, false, recursive, noInline, forceInline);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(ImSet<L> context, CalcPropertyMapImplement<?, L> forProp, ImOrderMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction, L addObject, CustomClass customClass, boolean forceDialog, boolean recursive, ImSet<L> noInline, boolean forceInline) {
        MSet<L> mInnerInterfaces = SetFact.mSet();
        mInnerInterfaces.addAll(context);
        mInnerInterfaces.addAll(forProp.mapping.valuesSet());
        mInnerInterfaces.addAll(action.mapping.valuesSet());
        if(elseAction != null)
            mInnerInterfaces.addAll(elseAction.mapping.valuesSet());
        if(addObject != null)
            mInnerInterfaces.add(addObject);
        mInnerInterfaces.addAll(getUsedInterfaces(orders.keys()));

        return createForAction(mInnerInterfaces.immutable(), context.toOrderSet(), forProp, orders, ordersNotNull, action, elseAction, addObject, customClass, forceDialog, recursive, noInline, forceInline);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(ImSet<L> innerInterfaces, ImSet<L> context, CalcPropertyMapImplement<?, L> forProp, ImOrderMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction, L addObject, CustomClass customClass, boolean forceDialog, boolean recursive, ImSet<L> noInline, boolean forceInline) {
        return createForAction(innerInterfaces, context.toOrderSet(), forProp, orders, ordersNotNull, action, elseAction, addObject, customClass, forceDialog, recursive, noInline, forceInline);
    }

    public static <L extends PropertyInterface> ActionPropertyMapImplement<?, L> createForAction(ImSet<L> innerInterfaces, ImOrderSet<L> mapInterfaces, CalcPropertyMapImplement<?, L> forProp, ImOrderMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, L> action, ActionPropertyMapImplement<?, L> elseAction, L addObject, CustomClass customClass, boolean forceDialog, boolean recursive, ImSet<L> noInline, boolean forceInline) {
        ForActionProperty<L> actionProperty = new ForActionProperty<L>("sys", innerInterfaces, mapInterfaces, forProp, orders, ordersNotNull, action, elseAction, addObject, customClass, forceDialog, recursive, noInline, forceInline);
        return actionProperty.getMapImplement();
    }

    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createSetAction(ImSet<L> context, CalcPropertyMapImplement<P, L> writeToProp, CalcPropertyInterfaceImplement<L> writeFrom) {
        return createSetAction(context, context, null, writeToProp, writeFrom);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createSetAction(ImSet<L> innerInterfaces, ImSet<L> context, CalcPropertyMapImplement<P, L> writeToProp, CalcPropertyInterfaceImplement<L> writeFrom) {
        return createSetAction(innerInterfaces, context, null, writeToProp, writeFrom);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createSetAction(ImSet<L> innerInterfaces, ImSet<L> context, CalcPropertyMapImplement<W, L> whereProp, CalcPropertyMapImplement<P, L> writeToProp, CalcPropertyInterfaceImplement<L> writeFrom) {
        return createSetAction(innerInterfaces, context.toOrderSet(), whereProp, writeToProp, writeFrom);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createSetAction(ImSet<L> innerInterfaces, ImOrderSet<L> mapInterfaces, CalcPropertyMapImplement<W, L> whereProp, CalcPropertyMapImplement<P, L> writeToProp, CalcPropertyInterfaceImplement<L> writeFrom) {
        SetActionProperty<P, W, L> actionProperty = new SetActionProperty<P, W, L>("sys", innerInterfaces, mapInterfaces, whereProp, writeToProp, writeFrom);
        return actionProperty.getMapImplement();
    }

    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createAddAction(CustomClass cls, boolean forceDialog, ImSet<L> innerInterfaces, ImSet<L> context, CalcPropertyMapImplement<W, L> whereProp, CalcPropertyMapImplement<P, L> resultProp, ImOrderMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull) {
        return createAddAction(cls, forceDialog, innerInterfaces, context.toOrderSet(), whereProp, resultProp, orders, ordersNotNull);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createAddAction(CustomClass cls, boolean forceDialog, ImSet<L> innerInterfaces, ImOrderSet<L> mapInterfaces, CalcPropertyMapImplement<W, L> whereProp, CalcPropertyMapImplement<P, L> resultProp, ImOrderMap<CalcPropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull) {
        AddObjectActionProperty<W, L> actionProperty = new AddObjectActionProperty<W, L>(cls, forceDialog, innerInterfaces, mapInterfaces, whereProp, resultProp, orders, ordersNotNull);
        return actionProperty.getMapImplement();
    }

    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createChangeClassAction(ObjectClass cls, boolean forceDialog, ImSet<L> innerInterfaces, ImSet<L> context, CalcPropertyMapImplement<W, L> whereProp, L changeInterface, BaseClass baseClass) {
        return createChangeClassAction(cls, forceDialog, innerInterfaces, context.toOrderSet(), whereProp, changeInterface, baseClass);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionPropertyMapImplement<?, L> createChangeClassAction(ObjectClass cls, boolean forceDialog, ImSet<L> innerInterfaces, ImOrderSet<L> mapInterfaces, CalcPropertyMapImplement<W, L> whereProp, L changeInterface, BaseClass baseClass) {
        ChangeClassActionProperty<W, L> actionProperty = new ChangeClassActionProperty<W, L>(cls, forceDialog, innerInterfaces, mapInterfaces, changeInterface, whereProp, baseClass);
        return actionProperty.getMapImplement();
    }

    // расширенный интерфейс создания SetAction, который умеет группировать, если что
//        FOR F(a,c,d,x) --- внеш. (e,x) + внутр. [a,c,d]
//            SET f(a,b) <- g(a,b,c,e)   --- внеш. (a,c,e) + внутр. [b]
//
//        SET f(a,b) <- [GROUP LAST F(a,c,d,x), g(a,b,c,e) ORDER O(a,c,d) BY a,b,c,e,x](a,b,c,e,x) WHERE [GROUP ANY F(a,c,d,x) BY a,c,x](a,c,x) --- внеш. (e,x) + внутр. [a,b,c]
    public static <W extends PropertyInterface, I extends PropertyInterface> ActionPropertyMapImplement<?, I> createSetAction(ImSet<I> context, CalcPropertyMapImplement<?, I> writeTo, CalcPropertyInterfaceImplement<I> writeFrom, CalcPropertyMapImplement<W, I> where, ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull) {
        ImSet<I> innerInterfaces = writeTo.mapping.valuesSet().merge(context);
        ImSet<I> whereInterfaces = where.mapping.valuesSet();
        assert innerInterfaces.merge(whereInterfaces).containsAll(getUsedInterfaces(writeFrom));

        if(!innerInterfaces.containsAll(whereInterfaces)) { // оптимизация, если есть допинтерфейсы - надо группировать
            if(!whereInterfaces.containsAll(getUsedInterfaces(writeFrom))) { // если не все ключи есть, придется докинуть or
                if(writeFrom instanceof CalcPropertyMapImplement) {
                    whereInterfaces = innerInterfaces.merge(whereInterfaces);
                    where = (CalcPropertyMapImplement<W, I>) SetActionProperty.getFullProperty(whereInterfaces, where, writeTo, writeFrom);
                } else { // по сути оптимизация, чтобы or не тянуть
                    whereInterfaces = whereInterfaces.merge((I) writeFrom);
                    where  = (CalcPropertyMapImplement<W, I>) createAnd(whereInterfaces, where, SetActionProperty.getValueClassProperty(writeTo, writeFrom));
                }
            }

            ImSet<I> checkContext = whereInterfaces.filter(context);
            if(!where.mapIsFull(checkContext)) // может быть избыточно для 2-го случая сверху, но для where в принципе надо
                where = (CalcPropertyMapImplement<W, I>) createAnd(whereInterfaces, where, IsClassProperty.getMapProperty(where.mapInterfaceClasses(ClassType.wherePolicy).filterIncl(checkContext)));

            ImRevMap<W, I> mapPushInterfaces = where.mapping.filterValuesRev(innerInterfaces); ImRevMap<I, W> mapWhere = where.mapping.reverse();
            writeFrom = createLastGProp(where.property, writeFrom.map(mapWhere), mapPushInterfaces.keys(), mapImplements(orders, mapWhere), ordersNotNull).map(mapPushInterfaces);
            where = (CalcPropertyMapImplement<W, I>) createAnyGProp(where.property, mapPushInterfaces.keys()).map(mapPushInterfaces);
        }

        return createSetAction(innerInterfaces, context, where, writeTo, writeFrom);
    }

    public static <W extends PropertyInterface, I extends PropertyInterface> ActionPropertyMapImplement<?, I> createChangeClassAction(ImSet<I> context, I changeInterface, ObjectClass cls, boolean forceDialog, CalcPropertyMapImplement<W, I> where, BaseClass baseClass, ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull) {
        ImSet<I> innerInterfaces = context.merge(changeInterface);
        ImSet<I> whereInterfaces = where.mapping.valuesSet();

        if(!innerInterfaces.containsAll(whereInterfaces)) { // оптимизация, если есть допинтерфейсы - надо группировать
            ImRevMap<W, I> mapPushInterfaces = where.mapping.filterValuesRev(innerInterfaces);
            where = (CalcPropertyMapImplement<W, I>) createAnyGProp(where.property, mapPushInterfaces.keys()).map(mapPushInterfaces);
        }

        return createChangeClassAction(cls, forceDialog, innerInterfaces, context, where, changeInterface, baseClass);
    }

}
