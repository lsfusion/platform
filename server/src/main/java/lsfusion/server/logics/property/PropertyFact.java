package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.expr.formula.CustomFormulaSyntax;
import lsfusion.server.data.expr.formula.FormulaUnionImpl;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.change.AddObjectAction;
import lsfusion.server.logics.action.change.ChangeClassAction;
import lsfusion.server.logics.action.change.SetAction;
import lsfusion.server.logics.action.flow.*;
import lsfusion.server.logics.action.implement.ActionImplement;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.action.session.action.ApplyAction;
import lsfusion.server.logics.action.session.action.NewSessionAction;
import lsfusion.server.logics.classes.StaticClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.DoubleClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.ObjectClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapChange;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapInput;
import lsfusion.server.logics.form.interactive.action.change.CheckCanBeChangedAction;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.input.PushRequestAction;
import lsfusion.server.logics.form.interactive.action.input.RequestAction;
import lsfusion.server.logics.property.cases.ActionCase;
import lsfusion.server.logics.property.cases.CalcCase;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.data.*;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.set.*;
import lsfusion.server.logics.property.value.NullValueProperty;
import lsfusion.server.logics.property.value.ValueProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.integration.internal.to.StringFormulaProperty;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

public class PropertyFact {
    private static StaticClass formulaClass = DoubleClass.instance;

    // общие методы

    private static PropertyImplement<CompareFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>> compareJoin(Compare compare, PropertyInterfaceImplement<JoinProperty.Interface> operator1, PropertyInterfaceImplement<JoinProperty.Interface> operator2) {
        CompareFormulaProperty compareProperty = new CompareFormulaProperty(compare);

        ImMap<CompareFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>> mapImplement =
                MapFact.toMap(compareProperty.operator1, operator1, compareProperty.operator2, operator2);

        return new PropertyImplement<>(compareProperty, mapImplement);
    }

    private static <P extends PropertyInterface, K extends PropertyInterface> Function<PropertyInterfaceImplement<K>, PropertyInterfaceImplement<P>> mapGetCalcValue(final ImRevMap<K, P> map) {
        return value -> value.map(map);
    }

    private static <P extends PropertyInterface, K extends PropertyInterface> Function<ActionMapImplement<?, K>, ActionMapImplement<?, P>> mapGetActionValue(final ImRevMap<K, P> map) {
        return value -> value.map(map);
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> ImCol<PropertyInterfaceImplement<K>> mapImplements(ImCol<? extends PropertyInterfaceImplement<T>> interfaceImplements, ImRevMap<T,K> map) {
        return ((ImCol<PropertyInterfaceImplement<T>>)interfaceImplements).mapColValues(PropertyFact.mapGetCalcValue(map));
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> ImList<PropertyInterfaceImplement<K>> mapImplements(ImList<? extends PropertyInterfaceImplement<T>> interfaceImplements, ImRevMap<T,K> map) {
        return ((ImList<PropertyInterfaceImplement<T>>)interfaceImplements).mapListValues(PropertyFact.mapGetCalcValue(map));
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> ImSet<PropertyInterfaceImplement<K>> mapImplements(ImSet<? extends PropertyInterfaceImplement<T>> interfaceImplements, ImRevMap<T,K> map) {
        return ((ImSet<PropertyInterfaceImplement<T>>)interfaceImplements).mapSetValues(PropertyFact.mapGetCalcValue(map));
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> ImOrderMap<PropertyInterfaceImplement<K>, Boolean> mapImplements(ImOrderMap<? extends PropertyInterfaceImplement<T>, Boolean> interfaceImplements, ImRevMap<T,K> map) {
        return ((ImOrderMap<PropertyInterfaceImplement<T>, Boolean>)interfaceImplements).mapOrderKeys(PropertyFact.mapGetCalcValue(map));
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface, C extends PropertyInterface> ImList<PropertyInterfaceImplement<K>> mapCalcImplements(ImRevMap<T, K> map, ImList<PropertyInterfaceImplement<T>> propertyImplements) {
        return mapImplements(propertyImplements, map);
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> ImList<ActionMapImplement<?, K>> mapActionImplements(ImRevMap<T,K> map, ImList<ActionMapImplement<?, T>> propertyImplements) {
        return propertyImplements.mapListValues(PropertyFact.mapGetActionValue(map));
    }

    public static <L,T extends PropertyInterface,K extends PropertyInterface> ImMap<L, PropertyInterfaceImplement<K>> mapImplements(ImMap<L, PropertyInterfaceImplement<T>> interfaceImplements, ImRevMap<T,K> map) {
        return interfaceImplements.mapValues(PropertyFact.mapGetCalcValue(map));
    }

    public static <T extends PropertyInterface,K extends PropertyInterface, P extends PropertyInterface> ActionImplement<P, PropertyInterfaceImplement<K>> mapActionImplements(ActionImplement<P, PropertyInterfaceImplement<T>> implement, ImRevMap<T,K> map) {
        return new ActionImplement<>(implement.action, mapImplements(implement.mapping, map));
    }

    public static <T extends PropertyInterface> ImSet<T> getUsedInterfaces(PropertyInterfaceImplement<T> interfaceImplement) {
        if(interfaceImplement instanceof PropertyMapImplement)
            return ((PropertyMapImplement<?, T>)interfaceImplement).mapping.valuesSet();
        else
            return SetFact.singleton((T) interfaceImplement);
    }

    public static <T extends PropertyInterface> ImSet<T> getUsedInterfaces(ImCol<? extends PropertyInterfaceImplement<T>> col) {
        MSet<T> mUsedInterfaces = SetFact.mSet();
        for(PropertyInterfaceImplement<T> interfaceImplement : col)
            mUsedInterfaces.addAll(getUsedInterfaces(interfaceImplement));
        return mUsedInterfaces.immutable();
    }

    // фильтрует только используемые интерфейсы и создает Join свойство с mapping'ом на эти интерфейсы
    public static <L extends PropertyInterface, T extends PropertyInterface> PropertyMapImplement<?,T> createJoin(PropertyImplement<L, PropertyInterfaceImplement<T>> implement) {
        // определяем какие интерфейсы использовали
        ImSet<T> usedInterfaces = getUsedInterfaces(implement.mapping.values());

        // создаем свойство - перемаппим интерфейсы
        ImRevMap<T,JoinProperty.Interface> joinMap = usedInterfaces.mapRevValues(JoinProperty.genInterface); // строим карту
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();
        JoinProperty<L> joinProperty = new JoinProperty<>(LocalizedString.NONAME, revJoinMap.keys().toOrderSet(),
                new PropertyImplement<>(implement.property, mapImplements(implement.mapping, joinMap)));
        return new PropertyMapImplement<>(joinProperty, revJoinMap);
    }

    // join when there are not all params are passed, and we have to create virtual ones
    public static <L extends PropertyInterface, T extends PropertyInterface> Pair<Property<JoinProperty.Interface>, ImRevMap<JoinProperty.Interface, T>> createPartJoin(PropertyImplement<L, PropertyInterfaceImplement<T>> implement) {
        ImSet<L> notUsedInterfaces = implement.property.interfaces.removeIncl(implement.mapping.keys());
        ImSet<T> usedInterfaces = getUsedInterfaces(implement.mapping.values());

        ImRevMap<L,JoinProperty.Interface> notUsedJoinMap = notUsedInterfaces.mapRevValues(JoinProperty.genInterface); // строим карту
        ImRevMap<T,JoinProperty.Interface> usedJoinMap = usedInterfaces.mapRevValues(JoinProperty.genInterface); // строим карту

        ImRevMap<JoinProperty.Interface, L> revNotUsedJoinMap = notUsedJoinMap.reverse();
        ImRevMap<JoinProperty.Interface, T> revUsedJoinMap = usedJoinMap.reverse();
        JoinProperty<L> joinProperty = new JoinProperty<>(LocalizedString.NONAME, revUsedJoinMap.keys().addExcl(revNotUsedJoinMap.keys()).toOrderSet(),
                new PropertyImplement<>(implement.property, mapImplements(implement.mapping, usedJoinMap).addExcl(notUsedJoinMap)));
        return new Pair<>(joinProperty, revUsedJoinMap);
    }

    public static <X extends PropertyInterface, T extends PropertyInterface> ActionMapImplement<?, T> createJoinAction(Action<X> action, PropertyMapImplement<?, T> implement) {
        return PropertyFact.createJoinAction(new ActionImplement<>(action, MapFact.singleton(action.interfaces.single(), implement)));
    }
    public static <L extends PropertyInterface, T extends PropertyInterface> ActionMapImplement<?,T> createJoinAction(ActionImplement<L, PropertyInterfaceImplement<T>> implement) {
        ImOrderSet<T> usedInterfaces = getUsedInterfaces(implement.mapping.values()).toOrderSet();
        JoinAction<L> joinProperty = new JoinAction<>(LocalizedString.NONAME, usedInterfaces, implement);
        return joinProperty.getImplement(usedInterfaces);
    }
    
    public static <T extends PropertyInterface> PropertyMapImplement<?, T> createCompare(Compare compare, T operator1, T operator2) {
        CompareFormulaProperty compareProperty = new CompareFormulaProperty(compare);

        ImRevMap<CompareFormulaProperty.Interface,T> mapImplement = MapFact.toRevMap(compareProperty.operator1, operator1, compareProperty.operator2, operator2);
        return new PropertyMapImplement<>(compareProperty, mapImplement);
    }
    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createCompare(LocalizedString caption, ImSet<T> interfaces, PropertyInterfaceImplement<T> propertyA, PropertyInterfaceImplement<T> propertyB, Compare compare) {
        ImRevMap<T, JoinProperty.Interface> joinMap = interfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();
        JoinProperty<CompareFormulaProperty.Interface> joinProperty = new JoinProperty<>(caption,
                revJoinMap.keys().toOrderSet(), compareJoin(compare, propertyA.map(joinMap), propertyB.map(joinMap)));
        return new PropertyMapImplement<>(joinProperty, revJoinMap);
    }
    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createCompare(ImSet<T> interfaces, PropertyInterfaceImplement<T> propertyA, PropertyInterfaceImplement<T> propertyB, Compare compare) {
        return createCompare(LocalizedString.NONAME, interfaces, propertyA, propertyB, compare);
    }
    public static <T extends PropertyInterface> PropertyMapImplement<PropertyInterface,T> createCompare(PropertyInterfaceImplement<T> propertyA, PropertyInterfaceImplement<T> propertyB, Compare compare) {
        return (PropertyMapImplement<PropertyInterface, T>) createCompare(getUsedInterfaces(SetFact.toSet(propertyA, propertyB)), propertyA, propertyB, compare);
    }
    public static <T extends PropertyInterface> PropertyMapImplement<PropertyInterface,T> createCompare(ImList<? extends PropertyInterfaceImplement<T>> propertiesA, ImList<? extends PropertyInterfaceImplement<T>> propertiesB, Compare compare) {
        return (PropertyMapImplement<PropertyInterface, T>) PropertyFact.createAnd(ListFact.toList(propertiesA.size(), i -> createCompare(propertiesA.get(i), propertiesB.get(i), compare)).getCol());
    }
    // needed because java cannot infer types
    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createCompareInterface(ImList<T> propertiesA, ImList<T> propertiesB, Compare compare) {
        return PropertyFact.createCompare(BaseUtils.<ImList<PropertyInterfaceImplement<T>>>immutableCast(propertiesA), BaseUtils.<ImList<PropertyInterfaceImplement<T>>>immutableCast(propertiesB), compare);
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?, T> createNot(ImSet<T> innerInterfaces, PropertyInterfaceImplement<T> implement) {
        ImRevMap<T, JoinProperty.Interface> joinMap = innerInterfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();

        return new PropertyMapImplement<>(new JoinProperty<>(LocalizedString.NONAME,
                revJoinMap.keys().toOrderSet(), NotFormulaProperty.instance.getImplement(implement.map(joinMap))), revJoinMap);
    }
    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createNot(PropertyInterfaceImplement<T> implement) {
        return createNot(getUsedInterfaces(implement), implement);
    }
    
    private static <T extends PropertyInterface> ImList<PropertyInterfaceImplement<T>> transNot(final ImList<PropertyInterfaceImplement<T>> ands, final ImList<Boolean> nots) {
        return ands.mapListValues((i, value) -> {
            if(nots.get(i))
                return createNot(value);
            return value;
        });
    }
    public static <P extends PropertyInterface> PropertyMapImplement<?,P> createAnd(ImOrderSet<P> listInterfaces, ImList<Boolean> nots) {
        return createAnd(LocalizedString.NONAME, listInterfaces.getSet(), listInterfaces.get(0), BaseUtils.<ImOrderSet<PropertyInterfaceImplement<P>>>immutableCast(listInterfaces.subList(1, listInterfaces.size())), nots);
    }

    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createAnd(LocalizedString caption, ImSet<T> interfaces, PropertyInterfaceImplement<T> object, ImList<PropertyInterfaceImplement<T>> ands, ImList<Boolean> nots) {
        return createAnd(caption, interfaces, object, transNot(ands, nots).getCol());
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createAnd(ImSet<T> interfaces, PropertyInterfaceImplement<T> object, ImList<PropertyInterfaceImplement<T>> ands, ImList<Boolean> nots) {
        return createAnd(LocalizedString.NONAME, interfaces, object, ands, nots);
    }
    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createAnd(ImSet<T> interfaces, PropertyInterfaceImplement<T> object, ImCol<? extends PropertyInterfaceImplement<T>> ands) {
        return createAnd(LocalizedString.NONAME, interfaces, object, ands);
    }
    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createAnd(ImCol<? extends PropertyInterfaceImplement<T>> ands) {
        return createAnd(getUsedInterfaces(ands), ands);
    }
    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createAnd(ImSet<T> interfaces, ImCol<? extends PropertyInterfaceImplement<T>> ands) {
        ImList<? extends PropertyInterfaceImplement<T>> andsList = ands.toList();
        return createAnd(interfaces, andsList.get(0), andsList.subList(1, andsList.size()).getCol());
    }
    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createAnd(LocalizedString caption, ImSet<T> interfaces, PropertyInterfaceImplement<T> object, final ImCol<? extends PropertyInterfaceImplement<T>> ands) {
        if(ands.size()==0 && object instanceof PropertyMapImplement)
            return (PropertyMapImplement<?,T>)object;

        final ImRevMap<T, JoinProperty.Interface> joinMap = interfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();

        AndFormulaProperty implement = new AndFormulaProperty(ands.size());
        ImMap<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>> joinImplement =
                        MapFact.addExcl(
                            implement.andInterfaces.mapValues(new IntFunction<PropertyInterfaceImplement<JoinProperty.Interface>>() {
                            public PropertyInterfaceImplement<JoinProperty.Interface> apply(int i) {
                                return ands.get(i).map(joinMap);
                            }}), implement.objectInterface,object.map(joinMap));

        JoinProperty<AndFormulaProperty.Interface> joinProperty = new JoinProperty<>(caption,
                revJoinMap.keys().toOrderSet(), new PropertyImplement<>(implement, joinImplement));
        return new PropertyMapImplement<>(joinProperty, revJoinMap);
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createAndNot(PropertyInterfaceImplement<T> object, PropertyInterfaceImplement<T> not) {
        return createAndNot(getUsedInterfaces(SetFact.toSet(object, not)), object, not);
    }
    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createAndNot(ImSet<T> innerInterfaces, PropertyInterfaceImplement<T> object, PropertyInterfaceImplement<T> not) {
        return createAnd(LocalizedString.NONAME, innerInterfaces, object, ListFact.singleton(not), ListFact.singleton(true));
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createAndNot(Property<T> property, PropertyInterfaceImplement<T> not) {
        return createAndNot(property.interfaces, property.getImplement(), not);
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createAnd(Property<T> property, PropertyInterfaceImplement<T> and) {
        return createAnd(property.interfaces, property.getImplement(), SetFact.singleton(and));
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createAnd(ImSet<T> interfaces, PropertyInterfaceImplement<T> object, PropertyInterfaceImplement<T> and) {
        return createAnd(interfaces, object, SetFact.singleton(and));
    }

    public static <MP extends PropertyInterface, MT extends PropertyInterface, P extends PropertyInterface, T extends PropertyInterface, C extends PropertyInterface> PropertyMapImplement<?,C> createAnd(PropertyInterfaceImplement<C> object, PropertyInterfaceImplement<C> and) {
        return createAnd(getUsedInterfaces(SetFact.toSet(object, and)), object, and);
    }

    public static <P extends PropertyInterface, T extends PropertyInterface, C extends PropertyInterface> ImRevMap<T, C> createCommon(ImSet<T> object, ImSet<P> and, final ImRevMap<T,P> map, final Result<ImRevMap<P, C>> mapAnd) {
        mapAnd.set(and.mapRevValues(() -> (C)new PropertyInterface()));
        return object.mapRevValues((Function<T, C>) value -> {
            P mp = map.get(value);
            if (mp != null)
                return mapAnd.result.get(mp);
            return (C) new PropertyInterface();
        });
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


    public static <T extends PropertyInterface, C extends PropertyInterface> PropertyMapImplement<?,T> createUnion(ImSet<T> interfaces, ImList<? extends PropertyInterfaceImplement<T>> props) {
        return createUnion(interfaces, props, false);
    }
    
    public static <T extends PropertyInterface, C extends PropertyInterface> PropertyMapImplement<?,T> createUnion(ImSet<T> interfaces, ImList<? extends PropertyInterfaceImplement<T>> props, boolean isExclusive) {
        ImRevMap<T,UnionProperty.Interface> mapInterfaces = interfaces.mapRevValues(UnionProperty.genInterface);
        ImRevMap<UnionProperty.Interface, T> revMapInterfaces = mapInterfaces.reverse();

        ImList<PropertyInterfaceImplement<UnionProperty.Interface>> operands =
                PropertyFact.mapCalcImplements(mapInterfaces, (ImList<PropertyInterfaceImplement<T>>) props);
        CaseUnionProperty unionProperty = new CaseUnionProperty(LocalizedString.NONAME, revMapInterfaces.keys().toOrderSet(), operands, false, isExclusive, false);
        return new PropertyMapImplement<>(unionProperty, revMapInterfaces);
    }

    public static <T extends PropertyInterface, C extends PropertyInterface> PropertyMapImplement<UnionProperty.Interface,T> createUnion(boolean checkExclusive, boolean checkAll, boolean isLast, CaseUnionProperty.Type type, ImSet<T> interfaces, ValueClass valueClass, ImMap<T, ValueClass> interfaceClasses) {
        ImRevMap<T,UnionProperty.Interface> mapInterfaces = interfaces.mapRevValues(UnionProperty.genInterface);
        ImRevMap<UnionProperty.Interface, T> revMapInterfaces = mapInterfaces.reverse();
        CaseUnionProperty unionProperty = new CaseUnionProperty(checkExclusive, checkAll, isLast, type, LocalizedString.NONAME, revMapInterfaces.keys().toOrderSet(), valueClass, revMapInterfaces.join(interfaceClasses));
        return new PropertyMapImplement<>(unionProperty, revMapInterfaces);
    }

    public static <T extends PropertyInterface, C extends PropertyInterface> PropertyMapImplement<?,T> createUnion(ImSet<T> interfaces, boolean isExclusive, ImList<CalcCase<T>> props) {
        final ImRevMap<T,UnionProperty.Interface> mapInterfaces = interfaces.mapRevValues(UnionProperty.genInterface);
        ImRevMap<UnionProperty.Interface, T> revMapInterfaces = mapInterfaces.reverse();

        ImList<CalcCase<UnionProperty.Interface>> cases = props.mapListValues((Function<CalcCase<T>, CalcCase<UnionProperty.Interface>>) value -> value.map(mapInterfaces));
        CaseUnionProperty unionProperty = new CaseUnionProperty(LocalizedString.NONAME, revMapInterfaces.keys().toOrderSet(), isExclusive, cases);
        return new PropertyMapImplement<>(unionProperty, revMapInterfaces);
    }

    public static <T extends PropertyInterface, C extends PropertyInterface> PropertyMapImplement<?,T> createXUnion(ImSet<T> interfaces, ImList<PropertyInterfaceImplement<T>> props) {
        ImRevMap<T,UnionProperty.Interface> mapInterfaces = interfaces.mapRevValues(UnionProperty.genInterface);
        ImRevMap<UnionProperty.Interface, T> revMapInterfaces = mapInterfaces.reverse();

        ImList<PropertyInterfaceImplement<UnionProperty.Interface>> operands = PropertyFact.mapCalcImplements(mapInterfaces, props);
        CaseUnionProperty unionProperty = new CaseUnionProperty(LocalizedString.NONAME, revMapInterfaces.keys().toOrderSet(),operands.getCol(), false);
        return new PropertyMapImplement<>(unionProperty, revMapInterfaces);
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?, T> createUnion(ImSet<T> interfaces, PropertyInterfaceImplement<T> first, PropertyInterfaceImplement<T> rest) {
        return createUnion(interfaces, ListFact.toList(first, rest));
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?, T> createXUnion(ImSet<T> interfaces, PropertyInterfaceImplement<T> first, PropertyInterfaceImplement<T> rest) {
        return createXUnion(interfaces, ListFact.toList(first, rest));
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?, PropertyInterface> createLogical(boolean value) {
        return value ? createTrue() : createFalse();
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createTrue() {
        return createStatic(true, LogicalClass.instance);
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createOne() {
        return createStatic(1L, LongClass.instance);
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?, T> createFalse() {
        return new PropertyMapImplement<>(new SessionDataProperty(LocalizedString.NONAME, LogicalClass.instance));
//        return new PropertyMapImplement<PropertyInterface, T>(NullValueProperty.instance, new HashMap<PropertyInterface, T>());
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createNotNull(PropertyInterfaceImplement<T> notNull) {
        return createAnd(getUsedInterfaces(notNull), PropertyFact.createTrue(), notNull);
    }
    public static Object getValueForProp(Object value, StaticClass objectClass) {
        if(objectClass instanceof StringClass)
            return LocalizedString.create((String)value, false);
        return value;
    }
    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createStatic(Object value, StaticClass valueClass) {
        return new PropertyMapImplement<>(new ValueProperty(LocalizedString.NONAME, value, valueClass), MapFact.EMPTYREV());
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createNull() {
        return new PropertyMapImplement<>(NullValueProperty.instance);
    }

    public static <T extends PropertyInterface> Property createAnyGProp(Property<T> property) {
        return createAnyGProp(property, SetFact.EMPTY()).property;
    }
    public static <T extends PropertyInterface, P extends PropertyInterface> PropertyMapImplement<?, T> createAnyGProp(PropertyMapImplement<P, T> implement, ImSet<T> groupInterfaces) {
        return createAnyGProp(implement.property, implement.mapping.filterInclValuesRev(groupInterfaces).keys()).map(implement.mapping);
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?, T> createAnyGProp(Property<T> prop, ImSet<T> groupInterfaces) {
        return createAnyGProp(LocalizedString.concatList("ANY ", prop.caption, " (" + groupInterfaces.toString(",") + ")"), prop, groupInterfaces);
    }
    public static <T extends PropertyInterface, N extends PropertyInterface> PropertyMapImplement<?, T> createAnyGProp(LocalizedString caption, Property<T> prop, ImSet<T> groupInterfaces) {
        if(!prop.getType().equals(LogicalClass.instance)) { // делаем Logical, может валиться по nullPointer, если в ACTION'е идут противоречивые условия (классы неправильные)
            PropertyMapImplement<N, T> notNull = (PropertyMapImplement<N, T>) PropertyFact.createNotNull(prop.getImplement());
            return PropertyFact.<N, T>createAnyGProp(caption, notNull.property, notNull.mapping.filterInclValuesRev(groupInterfaces).keys()).map(notNull.mapping);
        }
        MaxGroupProperty<T> groupProperty = new MaxGroupProperty<>(caption, BaseUtils.<ImSet<PropertyInterfaceImplement<T>>>immutableCast(groupInterfaces), prop, false);
        return new PropertyMapImplement<>(groupProperty, BaseUtils.<ImMap<GroupProperty.Interface<T>, T>>immutableCast(groupProperty.getMapInterfaces()).toRevExclMap());
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?, T> createLastGProp(Property<T> where, PropertyInterfaceImplement<T> last, ImSet<T> groupInterfaces, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        return createGProp(GroupType.LAST, where.interfaces, groupInterfaces, ListFact.toList(where.getImplement(), last), orders, ordersNotNull);
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?, T> createGProp(GroupType type, ImSet<T> innerInterfaces, ImSet<T> groupInterfaces, ImList<PropertyInterfaceImplement<T>> props, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        OrderGroupProperty<T> groupProperty = new OrderGroupProperty<>(LocalizedString.NONAME, innerInterfaces, BaseUtils.<ImCol<PropertyInterfaceImplement<T>>>immutableCast(groupInterfaces), props, type, orders, ordersNotNull);
        return new PropertyMapImplement<>(groupProperty, BaseUtils.<ImMap<GroupProperty.Interface<T>, T>>immutableCast(groupProperty.getMapInterfaces()).toRevExclMap());
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?, GroupProperty.Interface<T>> createSumGProp(ImSet<T> innerInterfaces, ImCol<? extends PropertyInterfaceImplement<T>> groupInterfaces, PropertyInterfaceImplement<T> property) {
        SumGroupProperty<T> groupProperty = new SumGroupProperty<>(LocalizedString.NONAME, innerInterfaces, groupInterfaces, property);
        return groupProperty.getImplement();
    }

    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createFormula(ImSet<T> interfaces, String formula, DataClass valueClass, ImList<? extends PropertyInterfaceImplement<T>> params) {
        return createFormula(LocalizedString.NONAME, interfaces, formula, valueClass, params);
    }
    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createFormula(LocalizedString caption, ImSet<T> interfaces, String formula, DataClass valueClass, ImList<? extends PropertyInterfaceImplement<T>> params) {
        final ImRevMap<T, JoinProperty.Interface> joinMap = interfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();

        final StringFormulaProperty implement = new StringFormulaProperty(valueClass,new CustomFormulaSyntax(formula),params.size(), false);
        ImMap<StringFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = 
                ((ImList<PropertyInterfaceImplement<T>>)params).mapListKeyValues((int i) -> implement.findInterface("prm"+(i+1)), mapGetCalcValue(joinMap));

        JoinProperty<StringFormulaProperty.Interface> joinProperty = new JoinProperty<>(caption,
                revJoinMap.keys().toOrderSet(), new PropertyImplement<>(implement, joinImplement));
        return new PropertyMapImplement<>(joinProperty, revJoinMap);
    }
    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createFormulaUnion(FormulaUnionImpl formula, ImList<? extends PropertyInterfaceImplement<T>> params) {
        ImSet<T> usedInterfaces = getUsedInterfaces(params.getCol());

        ImRevMap<T, UnionProperty.Interface> joinMap = usedInterfaces.mapRevValues(UnionProperty.genInterface);

        return new PropertyMapImplement<>(new FormulaUnionProperty(LocalizedString.NONAME, joinMap.valuesSet().toOrderSet(), mapImplements(params, joinMap), formula), joinMap.reverse());
    }

    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createConcatenate(LocalizedString caption, ImSet<T> interfaces, ImList<? extends PropertyInterfaceImplement<T>> params) {
        ImRevMap<T, JoinProperty.Interface> joinMap = interfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();

        final ConcatenateProperty implement = new ConcatenateProperty(params.size());
        ImMap<ConcatenateProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>> joinImplement =
                ((ImList<PropertyInterfaceImplement<T>>)params).mapListKeyValues(implement::getInterface, mapGetCalcValue(joinMap));

        JoinProperty<ConcatenateProperty.Interface> joinProperty = new JoinProperty<>(caption,
                revJoinMap.keys().toOrderSet(), new PropertyImplement<>(implement, joinImplement));
        return new PropertyMapImplement<>(joinProperty, revJoinMap);
    }

    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createDeconcatenate(LocalizedString caption, Property<T> property, int part, BaseClass baseClass) {
        ImRevMap<T, JoinProperty.Interface> joinMap = property.interfaces.mapRevValues(JoinProperty.genInterface);
        ImRevMap<JoinProperty.Interface, T> revJoinMap = joinMap.reverse();

        DeconcatenateProperty implement = new DeconcatenateProperty(part,baseClass);
        ImMap<DeconcatenateProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>> joinImplement = MapFact.singleton(implement.interfaces.single(), new PropertyMapImplement<>(property, joinMap));

        JoinProperty<DeconcatenateProperty.Interface> joinProperty = new JoinProperty<>(caption, revJoinMap.keys().toOrderSet(),
                new PropertyImplement<>(implement, joinImplement));
        return new PropertyMapImplement<>(joinProperty, revJoinMap);
    }

    // PARTITION методы

    private static <P extends PropertyInterface, T extends PropertyInterface> PropertyMapImplement<?,JoinProperty.Interface> createCompareProp(PropertyMapImplement<P, T> implement, ImRevMap<T, JoinProperty.Interface> map1, ImRevMap<T, JoinProperty.Interface> map2, ImRevMap<T, JoinProperty.Interface> mapCommon, Compare compare) {
        Result<ImRevMap<P,JoinProperty.Interface>> join1 = new Result<>(); Result<ImRevMap<P,JoinProperty.Interface>> join2 = new Result<>(); Result<ImRevMap<P, JoinProperty.Interface>> joinCommon = new Result<>();
        ImOrderSet<JoinProperty.Interface> listInterfaces = createSelfProp(implement.property.interfaces, join1, join2, joinCommon, implement.mapping.filterValuesRev(mapCommon.keys()).keys());
        JoinProperty<CompareFormulaProperty.Interface> joinProperty = new JoinProperty<>(LocalizedString.NONAME, listInterfaces,
                compareJoin(compare, new PropertyMapImplement<>(implement.property, join1.result.addRevExcl(joinCommon.result)), new PropertyMapImplement<>(implement.property, join2.result.addRevExcl(joinCommon.result))));
        
        return new PropertyMapImplement<>(joinProperty,
                join1.result.crossJoin(implement.mapping.join(map1)).addRevExcl(
                        join2.result.crossJoin(implement.mapping.join(map2))).addRevExcl(
                        joinCommon.result.crossJoin(implement.mapping.join(mapCommon))));
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> PropertyInterfaceImplement<JoinProperty.Interface> createInterfaceCompareMap(T implement, ImRevMap<T, JoinProperty.Interface> map1, ImRevMap<T, JoinProperty.Interface> map2, ImRevMap<T, JoinProperty.Interface> mapCommon, Compare compare) {
        JoinProperty.Interface commonInterface = mapCommon.get(implement);
        if(commonInterface!=null)
            return commonInterface;

        CompareFormulaProperty compareProperty = new CompareFormulaProperty(compare);
        ImRevMap<CompareFormulaProperty.Interface,JoinProperty.Interface> mapImplement = MapFact.toRevMap(compareProperty.operator1, map1.get(implement), compareProperty.operator2, map2.get(implement));
        return new PropertyMapImplement<>(compareProperty, mapImplement);
    }

    private static <P extends PropertyInterface, T extends PropertyInterface> PropertyInterfaceImplement<JoinProperty.Interface> createCompareMap(PropertyInterfaceImplement<T> implement, ImRevMap<T, JoinProperty.Interface> map1, ImRevMap<T, JoinProperty.Interface> map2, ImRevMap<T, JoinProperty.Interface> mapCommon, Compare compare) {
        if(implement instanceof PropertyMapImplement)
            return createCompareProp((PropertyMapImplement<?,T>)implement, map1, map2, mapCommon, compare);
        else
            return createInterfaceCompareMap((T)implement, map1, map2, mapCommon, compare);
    }

    public static <T extends PropertyInterface> JoinProperty<AndFormulaProperty.Interface> createPartition(ImSet<T> interfaces, PropertyInterfaceImplement<T> property, ImCol<PropertyInterfaceImplement<T>> partitions, final PropertyInterfaceImplement<T> expr, final Result<ImRevMap<T, JoinProperty.Interface>> mapMain, final Compare compare) {
        // "двоим" интерфейсы (для partition'а не PropertyInterface), для результ. св-ва
        MSet<T> mPartInterfaces = SetFact.mSet();
        MSet<PropertyMapImplement<?,T>> mPartProperties = SetFact.mSet();
        for(PropertyInterfaceImplement<T> partition : partitions)
            partition.fill(mPartInterfaces, mPartProperties);
        final ImSet<T> partInterfaces = mPartInterfaces.immutable();
        assert interfaces.containsAll(partInterfaces);
        final ImSet<PropertyMapImplement<?, T>> partProperties = mPartProperties.immutable();

        final Result<ImRevMap<T,JoinProperty.Interface>> map1 = new Result<>();
        final Result<ImRevMap<T,JoinProperty.Interface>> map2 = new Result<>();
        final Result<ImRevMap<T,JoinProperty.Interface>> mapCommon = new Result<>();
        ImOrderSet<JoinProperty.Interface> listInterfaces = createSelfProp(interfaces, map1, map2, mapCommon, partInterfaces);
        mapMain.set(map1.result.addRevExcl(mapCommon.result));

        // ставим equals'ы на partitions свойства (раздвоенные), greater на предшествие order (раздвоенное)
        AndFormulaProperty andPrevious = new AndFormulaProperty(partProperties.size() + 1);
        ImMap<AndFormulaProperty.Interface, PropertyInterfaceImplement<JoinProperty.Interface>> mapImplement =
                MapFact.addExcl(
                        andPrevious.andInterfaces.mapValues(new IntFunction<PropertyInterfaceImplement<JoinProperty.Interface>>() {
                            public PropertyInterfaceImplement<JoinProperty.Interface> apply(int i) {
                                return i == 0 ? createCompareMap(expr, map1.result, map2.result, mapCommon.result, compare) : createCompareProp(partProperties.get(i-1), map1.result, map2.result, mapCommon.result, Compare.EQUALS);
                            }
                        }), andPrevious.objectInterface, property.map(map2.result.addRevExcl(mapCommon.result)));
        return new JoinProperty<>(LocalizedString.NONAME, listInterfaces,
                new PropertyImplement<>(andPrevious, mapImplement));
    }

    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createOProp(Property<T> property, ImSet<PropertyInterfaceImplement<T>> partitions, ImOrderMap<PropertyInterfaceImplement<T>,Boolean> orders, boolean includeLast) {
        return createOProp(LocalizedString.NONAME, PartitionType.sum(), property, partitions, orders, includeLast);
    }
    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createOProp(LocalizedString caption, PartitionType partitionType, Property<T> property, ImSet<PropertyInterfaceImplement<T>> partitions, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean includeLast) {
        if(true) {
            ImList<PropertyInterfaceImplement<T>> propList = ListFact.singleton(property.getImplement());
            return createOProp(caption, partitionType, property.interfaces, propList, partitions, orders, Settings.get().isDefaultOrdersNotNull(), includeLast);
        }

        throw new UnsupportedOperationException();
 /*       assert orders.size()>0;
        
        if(orders.size()==1) return createSOProp(sID, caption, property, partitions, orders.singleKey(), orders.singleValue(), includeLast);
        // итеративно делаем Union, перекидывая order'ы в partition'ы
        ImOrderSet<UnionProperty.Interface> listInterfaces = UnionProperty.getInterfaces(property.interfaces.size());
        ImRevMap<T,UnionProperty.Interface> mapInterfaces = property.interfaces.toRevMap(listInterfaces);

        ImMap<PropertyInterfaceImplement<UnionProperty.Interface>, Integer> operands = new HashMap<PropertyInterfaceImplement<UnionProperty.Interface>, Integer>();
        Iterator<ImMap.Entry<PropertyInterfaceImplement<T>,Boolean>> it = orders.entrySet().iterator();
        while(it.hasNext()) {
            ImMap.Entry<PropertyInterfaceImplement<T>,Boolean> order = it.next();
            operands.put(createSOProp(property, partitions, order.getKey(), order.getValue(), it.hasNext() && includeLast).map(mapInterfaces),1);
            partitions = new ArrayList<PropertyInterfaceImplement<T>>(partitions);
            partitions.add(order.getKey());
        }

        return new PropertyMapImplement<UnionProperty.Interface,T>(new SumUnionProperty(sID,caption,listInterfaces,operands),BaseUtils.reverse(mapInterfaces));*/
    }

    public static <T extends PropertyInterface> PropertyMapImplement<?,T> createOProp(LocalizedString caption, PartitionType partitionType, ImSet<T> innerInterfaces, ImList<PropertyInterfaceImplement<T>> props, ImSet<PropertyInterfaceImplement<T>> partitions, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull, boolean includeLast) {
        PartitionProperty<T> orderProperty = new PartitionProperty<>(caption, partitionType, innerInterfaces, props, partitions, orders, ordersNotNull, includeLast);
        return new PropertyMapImplement<>(orderProperty, orderProperty.getMapInterfaces());
    }

    public static <T,V extends PropertyInterface> PropertyRevImplement<?,T> createCProp(LocalizedString caption, ImMap<T, ValueClass> params) {
        ImRevMap<PropertyInterface, T> mapInterfaces = params.keys().mapRevKeys((T value) -> new PropertyInterface());
        ImList<PropertyInterfaceImplement<PropertyInterface>> listImplements = mapInterfaces.join(params).mapColValues((BiFunction<PropertyInterface, ValueClass, PropertyInterfaceImplement<PropertyInterface>>) (key, value) -> IsClassProperty.getProperty(value, "value").mapPropertyImplement(MapFact.singletonRev("value", key))).toList();

        return createAnd(caption, mapInterfaces.keys(), listImplements.get(0), listImplements.subList(1, listImplements.size()).getCol()).mapRevImplement(mapInterfaces);
    }

    
    // строится partition с пустым order'ом
    private static <T extends PropertyInterface> PropertyMapImplement<?,T> createPProp(ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerInterfaces, PropertyInterfaceImplement<T> property, ImSet<PropertyInterfaceImplement<T>> partitions, GroupType type) {
        GroupProperty<T> partitionGroup = type.createProperty(LocalizedString.NONAME, innerInterfaces.getSet(), property, partitions);
        partitionGroup.setExplicitInnerClasses(innerInterfaces, explicitInnerInterfaces);
        return createJoin(new PropertyImplement<>(partitionGroup, partitionGroup.getMapInterfaces()));
    }

    public static <L extends PropertyInterface, T extends PropertyInterface> PropertyMapImplement<?,T> createUGProp(PropertyImplement<L, PropertyInterfaceImplement<T>> group, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, Property<T> restriction, boolean over) {
        return createUGProp(LocalizedString.NONAME, restriction.interfaces, group, orders, Settings.get().isDefaultOrdersNotNull(), restriction.getImplement(), over);
    }
    public static <L extends PropertyInterface, T extends PropertyInterface> PropertyMapImplement<?,T> createUGProp(LocalizedString caption, ImSet<T> innerInterfaces, PropertyImplement<L, PropertyInterfaceImplement<T>> group, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull, PropertyInterfaceImplement<T> restriction, boolean over) {
        ImSet<PropertyInterfaceImplement<T>> partitions = group.mapping.values().toSet();

        // строим связное distribute св-во, узнаем все использованные интерфейсы, строим map
        PropertyMapImplement<?, T> distribute = createJoin(group);

        if(true) {
            PartitionProperty<T> orderProperty = new PartitionProperty<>(caption, over ? PartitionType.distrRestrictOver() : PartitionType.distrRestrict(), innerInterfaces, ListFact.toList(restriction, distribute), partitions, orders, ordersNotNull, true);
            return new PropertyMapImplement<>(orderProperty, orderProperty.getMapInterfaces());
        }

        throw new RuntimeException("not supported");
/*
        // нужно MIN2(огр., распр. - пред.) И распр. > пред. причем пред. подходит и null, остальные не null
        // кроме того у огр. и пред. есть все интерфейсы, а у распр. - не все
        // старый вариант : пока  MIN2(огр., распр. - пред.) И распр. > пред. (1) ИЛИ MIN2(огр., распр.) И пред. null (2)
        // новый вариант : пред. = UNION (0 and огр., сум. без) или считаем (суи. с - огр.) = пред. ??? и зтем обычную формулу

        PropertyMapImplement<?, T> restImplement = restriction.getImplement();

        // считаем пред., тут 2 варианта
        PropertyMapImplement<?, T> previous;
        // через Union
        if(false) {
            // сум. без
            PropertyMapImplement<?, T> orderSum = createOProp(restriction, group.mapping.values(), orders, false);

            // 0 and огр
            PropertyMapImplement<?, T> firstZero = createAnd(restriction.interfaces, PropertyFact.<T>createStatic(0, formulaClass), restImplement);

            // UNION(0 and огр, сум. без)
            previous = createUnion(restriction.interfaces, firstZero, orderSum);
        } else {
            // сум. с
            PropertyMapImplement<?, T> orderSum = createOProp(restriction, group.mapping.values(), orders, true);

            // сум. с - огр.
            previous = createDiff(restriction, orderSum);
        }

        // MIN2(огр., распр. - пред.)
        PropertyMapImplement<?, T> min = createCustomFormula(restriction.interfaces, "(prm1+prm2-prm3-ABS(prm1-(prm2-prm3)))/2", formulaClass, BaseUtils.toList(restImplement, distribute, previous));

        // распр. > пред.
        PropertyMapImplement<?, T> compare = createCompare(restriction.interfaces, distribute, previous, Compare.GREATER);

        // MIN2(огр., распр. - пред.) И распр. > пред.
        return createAnd(restriction.interfaces, min, compare);*/
    }

    public static <L extends PropertyInterface, T extends PropertyInterface<T>> PropertyMapImplement<?,T> createPGProp(LocalizedString caption, int roundlen, boolean roundfirst, BaseClass baseClass, ImOrderSet<T> innerInterfaces, List<ResolveClassSet> explicitInnerInterfaces, PropertyImplement<L, PropertyInterfaceImplement<T>> group, PropertyInterfaceImplement<T> proportion, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {

        ImSet<PropertyInterfaceImplement<T>> partitions = group.mapping.values().toSet();

        // строим partition distribute св-во
        PropertyMapImplement<?, T> distribute = createJoin(group);

        if(roundfirst) {
            PartitionProperty<T> orderProperty = new PartitionProperty<>(caption, PartitionType.distrCumProportion(roundlen), innerInterfaces.getSet(), ListFact.toList(proportion, distribute), partitions, orders, ordersNotNull, false);
            return new PropertyMapImplement<>(orderProperty, orderProperty.getMapInterfaces());
        }

        // общая сумма по пропорции в partition'е
        PropertyMapImplement<?, T> propSum = createPProp(innerInterfaces, explicitInnerInterfaces, proportion, partitions, GroupType.SUM);

        LocalizedString distrCaption = !roundfirst ? caption : LocalizedString.NONAME;
        // округляем

        int numericLength = 15 + roundlen;
        PropertyMapImplement<?, T> distrRound =
                createFormula(distrCaption, innerInterfaces.getSet(),
                              "ROUND(CAST((prm1*prm2/prm3) as NUMERIC(" + numericLength + "," + roundlen+ "))," + roundlen + ")", NumericClass.get(numericLength, roundlen), ListFact.toList(distribute, proportion, propSum));

        if (!roundfirst) return distrRound;
        
        throw new RuntimeException("not supported");

/*        // строим partition полученного округления
        PropertyMapImplement<?, T> totRound = createPProp(distrRound, group.mapping.values(), GroupType.SUM);

        // получаем сколько надо дораспределить
        PropertyMapImplement<?, T> diffRound = createCustomFormula(proportion.interfaces, "prm1-prm2", formulaClass, BaseUtils.toList(distribute, totRound)); // вообще гря разные параметры поэтому formula, а не diff

        // берем первую пропорцию 
        PropertyMapImplement<?, T> proportionFirst = createFOProp(proportion, baseClass, group.mapping.values());

        // включаем всю оставшуюся сумму на нее
        PropertyMapImplement<?, T> diffFirst = createAnd(proportion.interfaces, diffRound, proportionFirst);

        // делаем union с основным distrRound'ом
        return createSum(sID, caption, proportion.interfaces, distrRound, diffFirst);*/
    }

    private static <T extends PropertyInterface> PropertyImplement<?, PropertyInterfaceImplement<T>> createMaxProp(LocalizedString caption, ImOrderSet<T> interfaces, List<ResolveClassSet> explicitInnerClasses, PropertyInterfaceImplement<T> implement, ImCol<PropertyInterfaceImplement<T>> group, boolean min) {
        MaxGroupProperty<T> maxProperty = new MaxGroupProperty<>(caption, interfaces.getSet(), group, implement, min);
        maxProperty.setExplicitInnerClasses(interfaces, explicitInnerClasses);        
        return new PropertyImplement<>(maxProperty, maxProperty.getMapInterfaces());
    }
    
    public static <T extends PropertyInterface> ImList<PropertyImplement<?, PropertyInterfaceImplement<T>>> createMGProp(LocalizedString[] captions, ImOrderSet<T> interfaces, List<ResolveClassSet> explicitInnerClasses, BaseClass baseClass, ImList<PropertyInterfaceImplement<T>> props, ImCol<PropertyInterfaceImplement<T>> group, MSet<Property> persist, boolean min) {
        if(props.size()==1)
            return createEqualsMGProp(captions, interfaces, explicitInnerClasses, props, group, persist, min);
        else
            return createConcMGProp(captions, interfaces, explicitInnerClasses, baseClass, props, group, persist, min);
    }

    private static <T extends PropertyInterface> ImList<PropertyImplement<?, PropertyInterfaceImplement<T>>> createEqualsMGProp(LocalizedString[] captions, ImOrderSet<T> interfaces, List<ResolveClassSet> explicitInnerClasses, ImList<PropertyInterfaceImplement<T>> props, ImCol<PropertyInterfaceImplement<T>> group, MSet<Property> persist, boolean min) {
        PropertyInterfaceImplement<T> propertyImplement = props.get(0);

        MList<PropertyImplement<?, PropertyInterfaceImplement<T>>> mResult = ListFact.mList();
        int i = 1;
        do {
            PropertyImplement<?, PropertyInterfaceImplement<T>> maxImplement = createMaxProp(captions[i-1], interfaces, explicitInnerClasses, propertyImplement, group, min);
            mResult.add(maxImplement);
            if(i<props.size()) { // если не последняя
                PropertyMapImplement<?,T> prevMax = createJoin(maxImplement); // какой максимум в partition'е

                PropertyMapImplement<?,T> equalsMax = createCompare(interfaces.getSet(), propertyImplement, prevMax, Compare.EQUALS);

                propertyImplement = createAnd(interfaces.getSet(), props.get(i), equalsMax);
            }
        } while (i++<props.size());
        return mResult.immutableList();
    }

    private static <T extends PropertyInterface,L extends PropertyInterface> ImList<PropertyImplement<?, PropertyInterfaceImplement<T>>> createDeconcatenate(LocalizedString[] captions, PropertyImplement<L, PropertyInterfaceImplement<T>> implement, int parts, BaseClass baseClass) {
        MList<PropertyImplement<?, PropertyInterfaceImplement<T>>> mResult = ListFact.mList(parts);
        for(int i=0;i<parts;i++)
            mResult.add(createDeconcatenate(captions[i], implement.property, i, baseClass).mapImplement(implement.mapping));
        return mResult.immutableList();
    }

    private static <T extends PropertyInterface> ImList<PropertyImplement<?, PropertyInterfaceImplement<T>>> createConcMGProp(LocalizedString[] captions, ImOrderSet<T> interfaces, List<ResolveClassSet> explicitInnerClasses, BaseClass baseClass, ImList<PropertyInterfaceImplement<T>> props, ImCol<PropertyInterfaceImplement<T>> group, MSet<Property> persist, boolean min) {
        String concCaption = BaseUtils.toString(", ", captions);

        PropertyMapImplement<?, T> concate = createConcatenate(LocalizedString.create("Concatenate - " + concCaption), interfaces.getSet(), props);
        persist.add(concate.property);
        
        PropertyImplement<?, PropertyInterfaceImplement<T>> max = createMaxProp(LocalizedString.create("Concatenate - " + concCaption), interfaces, explicitInnerClasses, concate, group, min);
        persist.add(max.property);

        return createDeconcatenate(captions, max, props.size(), baseClass);
    }

    public static <L extends PropertyInterface> PropertyMapImplement<?, L> createIfElseUProp(ImSet<L> innerInterfaces, PropertyInterfaceImplement<L> ifProp, PropertyMapImplement<?, L> trueProp, PropertyMapImplement<?, L> falseProp) {
        PropertyMapImplement<?, L> ifTrue = null;
        if(trueProp!=null)
            ifTrue = PropertyFact.createAnd(innerInterfaces, trueProp, ifProp);
        PropertyMapImplement<?, L> ifFalse = null;
        if(falseProp!=null)
            ifFalse = PropertyFact.createAndNot(innerInterfaces, falseProp, ifProp);
        
        if(ifTrue==null)
            return ifFalse;
        if(ifFalse==null)
            return ifTrue;
        return PropertyFact.createXUnion(innerInterfaces, ifTrue, ifFalse);
    }

    public static SessionDataProperty createInputDataProp(ValueClass valueClass) {
        return new SessionDataProperty(LocalizedString.NONAME, valueClass);
    }

    public static SessionDataProperty createImportDataProp(ValueClass valueClass, ImList<ValueClass> paramClasses) {
        return new SessionDataProperty(LocalizedString.NONAME, paramClasses.toArray(new ValueClass[paramClasses.size()]), valueClass);
    }

    public static <T extends PropertyInterface> PropertyMapImplement<ClassPropertyInterface, T> createForDataProp(ImMap<T, ValueClass> interfaces, ValueClass valueClass, MSet<SessionDataProperty> mLocals) {
        ImOrderMap<T, ValueClass> orderInterfaces = interfaces.toOrderMap();
        ImOrderSet<T> listInterfaces = orderInterfaces.keyOrderSet();
        ValueClass[] interfaceClasses = orderInterfaces.valuesList().toArray(new ValueClass[orderInterfaces.size()]);
        SessionDataProperty dataProperty = new SessionDataProperty(LocalizedString.NONAME, interfaceClasses, valueClass, true);
        mLocals.add(dataProperty);
        return dataProperty.getImplement(listInterfaces);
    }

    public static <T> PropertyRevImplement<ClassPropertyInterface, T> createDataPropRev(LocalizedString caption, ImMap<T, ValueClass> interfaces, ValueClass valueClass, LocalNestedType nestedType) {
        ImOrderMap<T, ValueClass> orderInterfaces = interfaces.toOrderMap();
        ImOrderSet<T> listInterfaces = orderInterfaces.keyOrderSet();
        ValueClass[] interfaceClasses = orderInterfaces.valuesList().toArray(new ValueClass[orderInterfaces.size()]);
        SessionDataProperty dataProperty = new SessionDataProperty(caption, interfaceClasses, valueClass);
        dataProperty.nestedType = nestedType;
        return dataProperty.getRevImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionMapImplement<?, L> createIfAction(ImSet<L> innerInterfaces, PropertyMapImplement<?, L> where, ActionMapImplement<?, L> action, ActionMapImplement<?, L> elseAction) {
        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        Action caseAction = CaseAction.createIf(LocalizedString.NONAME, false, listInterfaces, where, action, elseAction);
        return caseAction.getImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionMapImplement<?, L> createTryAction(ImSet<L> innerInterfaces, ActionMapImplement<?, L> tryAction, ActionMapImplement<?, L> catchAction, ActionMapImplement<?, L> finallyAction) {
        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        Action caseAction = new TryAction(LocalizedString.NONAME, listInterfaces, tryAction, catchAction, finallyAction);
        return caseAction.getImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionMapImplement<?, L> createCaseAction(ImSet<L> innerInterfaces, boolean isExclusive, ImList<ActionCase<L>> cases) {
        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        Action caseAction = new CaseAction(LocalizedString.NONAME, isExclusive, listInterfaces, cases);
        return caseAction.getImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionMapImplement<?, L> createEmptyAction() {
        return (ActionMapImplement<?,L>)createListAction(SetFact.EMPTY(), ListFact.EMPTY());
    }
    
    public static <L extends PropertyInterface> ActionMapImplement<?, L> createListAction(ImSet<L> innerInterfaces, ActionMapImplement<?, L>... actions) {
        return createListAction(innerInterfaces, ListFact.toList(actions));
    }
    public static <L extends PropertyInterface> ActionMapImplement<?, L> createListAction(ImSet<L> innerInterfaces, ImList<ActionMapImplement<?, L>> actions) {
        return createListAction(innerInterfaces, actions, SetFact.EMPTY());
    }

    public static <L extends PropertyInterface> ActionMapImplement<?, L> createListAction(ImSet<L> innerInterfaces, ImList<ActionMapImplement<?, L>> actions, ImSet<SessionDataProperty> localsInScope) {
        if(actions.size()==1 && localsInScope.isEmpty()) {
            ActionMapImplement<?, L> singleAction = actions.single();
            assert innerInterfaces.containsAll(singleAction.mapping.valuesSet());
            if(singleAction.mapping.size() == innerInterfaces.size())
                return singleAction;
        }

        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        ListAction action = new ListAction(LocalizedString.NONAME, listInterfaces, actions, localsInScope);
        return action.getImplement(listInterfaces);
    }

    public static <L extends PropertyInterface> ActionMapImplement<?, L> createForAction(ImSet<L> context, PropertyMapImplement<?, L> forProp, ImOrderMap<PropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionMapImplement<?, L> action, ActionMapImplement<?, L> elseAction, boolean recursive, ImSet<L> noInline, boolean forceInline) {
        return createForAction(context, forProp, orders, ordersNotNull, action, elseAction, null, null, false, recursive, noInline, forceInline);
    }

    public static <L extends PropertyInterface> ActionMapImplement<?, L> createForAction(ImSet<L> innerInterfaces, ImSet<L> context, PropertyMapImplement<?, L> forProp, ImOrderMap<PropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionMapImplement<?, L> action, ActionMapImplement<?, L> elseAction, boolean recursive, ImSet<L> noInline, boolean forceInline) {
        return createForAction(innerInterfaces, context, forProp, orders, ordersNotNull, action, elseAction, null, null, false, recursive, noInline, forceInline);
    }

    public static <L extends PropertyInterface> ActionMapImplement<?, L> createForAction(ImSet<L> innerInterfaces, ImSet<L> context, ActionMapImplement<?, L> action, L addObject, ConcreteCustomClass customClass, boolean recursive) {
        return createForAction(innerInterfaces, context, action, addObject, customClass, recursive, SetFact.EMPTY(), false);
    }

    public static <L extends PropertyInterface> ActionMapImplement<?, L> createForAction(ImSet<L> innerInterfaces, ImSet<L> context, ActionMapImplement<?, L> action, L addObject, CustomClass customClass, boolean recursive, ImSet<L> noInline, boolean forceInline) {
        return createForAction(innerInterfaces, context, null, MapFact.EMPTYORDER(), false, action, null, addObject, customClass, false, recursive, noInline, forceInline);
    }

    public static <L extends PropertyInterface> ActionMapImplement<?, L> createForAction(ImSet<L> context, PropertyMapImplement<?, L> forProp, ImOrderMap<PropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionMapImplement<?, L> action, ActionMapImplement<?, L> elseAction, L addObject, CustomClass customClass, boolean autoSet, boolean recursive, ImSet<L> noInline, boolean forceInline) {
        MSet<L> mInnerInterfaces = SetFact.mSet();
        mInnerInterfaces.addAll(context);
        mInnerInterfaces.addAll(forProp.mapping.valuesSet());
        mInnerInterfaces.addAll(action.mapping.valuesSet());
        if(elseAction != null)
            mInnerInterfaces.addAll(elseAction.mapping.valuesSet());
        if(addObject != null)
            mInnerInterfaces.add(addObject);
        mInnerInterfaces.addAll(getUsedInterfaces(orders.keys()));

        return createForAction(mInnerInterfaces.immutable(), context.toOrderSet(), forProp, orders, ordersNotNull, action, elseAction, addObject, customClass, autoSet, recursive, noInline, forceInline);
    }

    public static <L extends PropertyInterface> ActionMapImplement<?, L> createForAction(ImSet<L> innerInterfaces, ImSet<L> context, PropertyMapImplement<?, L> forProp, ImOrderMap<PropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionMapImplement<?, L> action, ActionMapImplement<?, L> elseAction, L addObject, CustomClass customClass, boolean autoSet, boolean recursive, ImSet<L> noInline, boolean forceInline) {
        return createForAction(innerInterfaces, context.toOrderSet(), forProp, orders, ordersNotNull, action, elseAction, addObject, customClass, autoSet, recursive, noInline, forceInline);
    }

    public static <L extends PropertyInterface> ActionMapImplement<?, L> createForAction(ImSet<L> innerInterfaces, ImOrderSet<L> mapInterfaces, PropertyMapImplement<?, L> forProp, ImOrderMap<PropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, ActionMapImplement<?, L> action, ActionMapImplement<?, L> elseAction, L addObject, CustomClass customClass, boolean autoSet, boolean recursive, ImSet<L> noInline, boolean forceInline) {
        ForAction<L> forAction = new ForAction<>(LocalizedString.NONAME, innerInterfaces, mapInterfaces, forProp, orders, ordersNotNull, action, elseAction, addObject, customClass, autoSet, recursive, noInline, forceInline);
        return forAction.getMapImplement();
    }

    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionMapImplement<?, L> createSetAction(ImSet<L> context, PropertyMapImplement<P, L> writeToProp, PropertyInterfaceImplement<L> writeFrom) {
        return createSetAction(context, context, null, writeToProp, writeFrom);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionMapImplement<?, L> createSetAction(ImSet<L> innerInterfaces, ImSet<L> context, PropertyMapImplement<P, L> writeToProp, PropertyInterfaceImplement<L> writeFrom) {
        return createSetAction(innerInterfaces, context, null, writeToProp, writeFrom);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionMapImplement<?, L> createSetAction(ImSet<L> innerInterfaces, ImSet<L> context, PropertyMapImplement<W, L> whereProp, PropertyMapImplement<P, L> writeToProp, PropertyInterfaceImplement<L> writeFrom) {
        return createSetAction(innerInterfaces, context.toOrderSet(), whereProp, writeToProp, writeFrom);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionMapImplement<?, L> createSetAction(ImSet<L> innerInterfaces, ImOrderSet<L> mapInterfaces, PropertyMapImplement<W, L> whereProp, PropertyMapImplement<P, L> writeToProp, PropertyInterfaceImplement<L> writeFrom) {
        SetAction<P, W, L> setAction = new SetAction<>(LocalizedString.NONAME, innerInterfaces, mapInterfaces, whereProp, writeToProp, writeFrom);
        return setAction.getMapImplement();
    }

    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionMapImplement<?, L> createAddAction(CustomClass cls, ImSet<L> innerInterfaces, ImSet<L> context, PropertyMapImplement<W, L> whereProp, PropertyMapImplement<P, L> resultProp, ImOrderMap<PropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, boolean autoSet) {
        return createAddAction(cls, innerInterfaces, context.toOrderSet(), whereProp, resultProp, orders, ordersNotNull, autoSet);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionMapImplement<?, L> createAddAction(CustomClass cls, ImSet<L> innerInterfaces, ImOrderSet<L> mapInterfaces, PropertyMapImplement<W, L> whereProp, PropertyMapImplement<P, L> resultProp, ImOrderMap<PropertyInterfaceImplement<L>, Boolean> orders, boolean ordersNotNull, boolean autoSet) {
        AddObjectAction<W, L> action = new AddObjectAction<>(cls, innerInterfaces, mapInterfaces, whereProp, resultProp, orders, ordersNotNull, autoSet);
        return action.getMapImplement();
    }

    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionMapImplement<?, L> createChangeClassAction(ObjectClass cls, boolean forceDialog, ImSet<L> innerInterfaces, ImSet<L> context, PropertyMapImplement<W, L> whereProp, L changeInterface, BaseClass baseClass) {
        return createChangeClassAction(cls, forceDialog, innerInterfaces, context.toOrderSet(), whereProp, changeInterface, baseClass);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionMapImplement<?, L> createChangeClassAction(ObjectClass cls, boolean forceDialog, ImSet<L> innerInterfaces, ImOrderSet<L> mapInterfaces, PropertyMapImplement<W, L> whereProp, L changeInterface, BaseClass baseClass) {
        ChangeClassAction<W, L> action = new ChangeClassAction<>(cls, forceDialog, innerInterfaces, mapInterfaces, changeInterface, whereProp, baseClass);
        return action.getMapImplement();
    }

    public static <X extends PropertyInterface, T extends PropertyInterface> void setResetAsync(Action<X> action, AsyncMapChange<T, X> asyncResetExec) {
        action.setForceAsyncEventExec(asyncExec -> {
            if(asyncExec instanceof AsyncMapInput) {
                asyncExec = ((AsyncMapInput<X>) asyncExec).override("reset", asyncResetExec);
            }
            return asyncExec;
        });
    }

    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionMapImplement<?, L> createRequestAction(ImSet<L> innerInterfaces, ActionMapImplement<?, L> requestAction, ActionMapImplement<?, L> doAction, ActionMapImplement<?, L> elseAction) {
        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        RequestAction setAction = new RequestAction(LocalizedString.NONAME, listInterfaces, requestAction, doAction, elseAction);
        return setAction.getImplement(listInterfaces);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionMapImplement<?, L> createCheckCanBeChangedAction(ImSet<L> innerInterfaces, PropertyMapImplement<?, L> changeProp) {
        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        CheckCanBeChangedAction changeAction = new CheckCanBeChangedAction(LocalizedString.NONAME, listInterfaces, changeProp);
        return changeAction.getImplement(listInterfaces);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface, W extends PropertyInterface> ActionMapImplement<?, L> createPushRequestAction(ImSet<L> innerInterfaces, ActionMapImplement<?, L> action) {
        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        PushRequestAction changeAction = new PushRequestAction(LocalizedString.NONAME, listInterfaces, action);
        return changeAction.getImplement(listInterfaces);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface> ActionMapImplement<?, L> createNewSessionAction(ImSet<L> innerInterfaces, ActionMapImplement<?, L> action, boolean singleApply, boolean newSQL, FunctionSet<SessionDataProperty> migrateSessionProperties, boolean isNested) {
        return createNewSessionAction(innerInterfaces, action, LocalizedString.NONAME, singleApply, newSQL, migrateSessionProperties, isNested);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface> ActionMapImplement<?, L> createNewSessionAction(ImSet<L> innerInterfaces, ActionMapImplement<?, L> action, LocalizedString caption, boolean singleApply, boolean newSQL, FunctionSet<SessionDataProperty> migrateSessionProperties, boolean isNested) {
        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        NewSessionAction aggAction = new NewSessionAction(caption, listInterfaces, action, singleApply, newSQL, migrateSessionProperties, isNested);
        return aggAction.getImplement(listInterfaces);
    }
    public static <T extends PropertyInterface> ActionMapImplement<?, T> createSessionScopeAction(FormSessionScope scope, ImSet<T> innerInterfaces, ActionMapImplement<?, T> action, FunctionSet<SessionDataProperty> migrateSessionProps) {
        return createSessionScopeAction(scope, innerInterfaces, action, LocalizedString.NONAME, migrateSessionProps);
    }
    public static <T extends PropertyInterface> ActionMapImplement<?, T> createSessionScopeAction(FormSessionScope scope, ImSet<T> innerInterfaces, ActionMapImplement<?, T> action, LocalizedString caption, FunctionSet<SessionDataProperty> migrateSessionProps) {
        if(scope.isNewSession())
            return createNewSessionAction(innerInterfaces, action, caption, false, false, migrateSessionProps, scope.isNestedSession());
        return action;
    }
    public static <L extends PropertyInterface, P extends PropertyInterface> ActionMapImplement<?, L> createNewThreadAction(ImSet<L> innerInterfaces, ActionMapImplement<?, L> action, PropertyInterfaceImplement<L> period, PropertyInterfaceImplement<L> delay, PropertyInterfaceImplement<L> connection) {
        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        NewThreadAction aggAction = new NewThreadAction(LocalizedString.NONAME, listInterfaces, action, period, delay, connection);
        return aggAction.getImplement(listInterfaces);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface> ActionMapImplement<?, L> createNewExecutorAction(ImSet<L> innerInterfaces, ActionMapImplement<?, L> action, PropertyInterfaceImplement<L> threads) {
        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        NewExecutorAction aggAction = new NewExecutorAction(LocalizedString.NONAME, listInterfaces, action, threads);
        return aggAction.getImplement(listInterfaces);
    }
    public static <L extends PropertyInterface, P extends PropertyInterface> ActionMapImplement<?, L> createApplyAction(ImSet<L> innerInterfaces, ActionMapImplement<?, L> action, FunctionSet<SessionDataProperty> keepSessionProperties, boolean serializable, Property canceled, Property applyMessage) {
        ImOrderSet<L> listInterfaces = innerInterfaces.toOrderSet();
        ApplyAction aggAction = new ApplyAction(LocalizedString.NONAME, listInterfaces, action, keepSessionProperties, serializable, canceled, applyMessage);
        return aggAction.getImplement(listInterfaces);
    }

    // расширенный интерфейс создания SetAction, который умеет группировать, если что
//        FOR F(a,c,d,x) --- внеш. (e,x) + внутр. [a,c,d]
//            SET f(a,b) <- g(a,b,c,e)   --- внеш. (a,c,e) + внутр. [b]
//
//        SET f(a,b) <- [GROUP LAST F(a,c,d,x), g(a,b,c,e) ORDER O(a,c,d) BY a,b,c,e,x](a,b,c,e,x) WHERE [GROUP ANY F(a,c,d,x) BY a,c,x](a,c,x) --- внеш. (e,x) + внутр. [a,b,c]
    public static <W extends PropertyInterface, I extends PropertyInterface> ActionMapImplement<?, I> createSetAction(ImSet<I> context, PropertyMapImplement<?, I> writeTo, PropertyInterfaceImplement<I> writeFrom, PropertyMapImplement<W, I> where, ImOrderMap<PropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull) {
        ImSet<I> innerInterfaces = writeTo.mapping.valuesSet().merge(context);
        ImSet<I> whereInterfaces = where.mapping.valuesSet();
        assert innerInterfaces.merge(whereInterfaces).containsAll(getUsedInterfaces(writeFrom));

        if(!innerInterfaces.containsAll(whereInterfaces)) { // оптимизация, если есть допинтерфейсы - надо группировать
            if(!whereInterfaces.containsAll(getUsedInterfaces(writeFrom))) { // если не все ключи есть, придется докинуть or
                if(writeFrom instanceof PropertyMapImplement) {
                    whereInterfaces = innerInterfaces.merge(whereInterfaces);
                    where = (PropertyMapImplement<W, I>) SetAction.getFullProperty(whereInterfaces, where, writeTo, writeFrom);
                } else { // по сути оптимизация, чтобы or не тянуть
                    whereInterfaces = whereInterfaces.merge((I) writeFrom);
                    where  = (PropertyMapImplement<W, I>) createAnd(whereInterfaces, where, SetAction.getValueClassProperty(writeTo, writeFrom));
                }
            }

            ImSet<I> checkContext = whereInterfaces.filter(context);
            // тут с assertion'ом на filterIncl есть нюанс, может получиться что контекст определяется сверху и он может проталкиваться, но на момент компиляции его нет (или может вообще не проталкиваться, тогда что делать непонятно)
            // при этом ситуацию усугубляет например если есть FOR t==x(d) NEW z=Z и d из верхнего контекста, условие x(d) скомпилируется в NEW и тип его не вытянется
            // поэтому будем подставлять те классы которые есть, предполагая что если нет они должны придти сверху
            if(!where.mapIsFull(checkContext)) // может быть избыточно для 2-го случая сверху, но для where в принципе надо
                where = (PropertyMapImplement<W, I>) createAnd(whereInterfaces, where, IsClassProperty.getMapProperty(where.mapInterfaceClasses(ClassType.wherePolicy).filter(checkContext))); // filterIncl

            ImRevMap<W, I> mapPushInterfaces = where.mapping.filterValuesRev(innerInterfaces); ImRevMap<I, W> mapWhere = where.mapping.reverse();
            writeFrom = createLastGProp(where.property, writeFrom.map(mapWhere), mapPushInterfaces.keys(), mapImplements(orders, mapWhere), ordersNotNull).map(mapPushInterfaces);
            where = (PropertyMapImplement<W, I>) createAnyGProp(where.property, mapPushInterfaces.keys()).map(mapPushInterfaces);
        }

        return createSetAction(innerInterfaces, context, where, writeTo, writeFrom);
    }

    public static <W extends PropertyInterface, I extends PropertyInterface> ActionMapImplement<?, I> createChangeClassAction(ImSet<I> context, I changeInterface, ObjectClass cls, boolean forceDialog, PropertyMapImplement<W, I> where, BaseClass baseClass, ImOrderMap<PropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull) {
        ImSet<I> innerInterfaces = context.merge(changeInterface);
        ImSet<I> whereInterfaces = where.mapping.valuesSet();

        if(!innerInterfaces.containsAll(whereInterfaces)) { // оптимизация, если есть допинтерфейсы - надо группировать
            ImRevMap<W, I> mapPushInterfaces = where.mapping.filterValuesRev(innerInterfaces);
            where = (PropertyMapImplement<W, I>) createAnyGProp(where.property, mapPushInterfaces.keys()).map(mapPushInterfaces);
        }

        return createChangeClassAction(cls, forceDialog, innerInterfaces, context, where, changeInterface, baseClass);
    }

    private static <P extends PropertyInterface> PropertyMapImplement<?, P> getFullWhereProperty(ImSet<P> innerInterfaces, PropertyInterfaceImplement<P> where, ImCol<PropertyInterfaceImplement<P>> exprs) {
        PropertyMapImplement<?, P> result = createUnion(innerInterfaces, exprs.mapColValues((Function<PropertyInterfaceImplement<P>, PropertyInterfaceImplement<P>>) PropertyFact::createNotNull).toList());
        if (where != null)
            result = PropertyFact.createAnd(innerInterfaces, where, result);
        return result;
    }

    public static <P extends PropertyInterface> PropertyInterfaceImplement<P> getFullWhereProperty(ImSet<P> innerInterfaces, ImSet<P> mapInterfaces, PropertyInterfaceImplement<P> where, ImCol<PropertyInterfaceImplement<P>> exprs) {
        ImSet<P> extInterfaces = innerInterfaces.remove(mapInterfaces);
        return (where == null && extInterfaces.isEmpty()) || (where != null && where.mapIsFull(extInterfaces)) ?
                (where == null ? PropertyFact.createTrue() : where) : getFullWhereProperty(innerInterfaces, where, exprs);
    }

    static <X extends PropertyInterface, T extends PropertyInterface> PropertyMapImplement<?, T> createViewProperty(ImList<Property> viewProperties) {
        PropertyMapImplement<?, T> resultValue = null;
        for(int i = viewProperties.size()-1; i>=0; i--) {
            Property<X> viewProperty = viewProperties.get(i);
            resultValue = resultValue == null ? ((Property<T>)viewProperty).getImplement() : createJoin(new PropertyImplement<>(viewProperty, MapFact.singleton(viewProperty.interfaces.single(), resultValue)));
        }
        return resultValue;
    }

    private static class CacheResult<T> {
        public final ImList<ImList<ImMap<?, T>>> properties;
        public final PropertyImplement<?, T> result;

        public CacheResult(ImList<ImList<ImMap<?, T>>> properties, PropertyImplement<?, T> result) {
            this.properties = properties;
            this.result = result;
        }

        public <K> PropertyImplement<?, K> map(ImList<ImList<ImMap<?, K>>> mapProperties) {

            Map<T, K> mapResult = new HashMap<>();
            if(map(properties, mapProperties, 0, 0, null, mapResult))
                return result.mapImplement(MapFact.fromJavaMap(mapResult));

            return null;
        }

        // assert that in the same list ? are the same
        private static <T, K, X> boolean map(ImList<ImList<ImMap<?, T>>> setProps, ImList<ImList<ImMap<?, K>>> mapSetProps, int index, int listIndex, boolean[] listMapped, Map<T, K> mappedKeys) {
            ImList<ImMap<?, T>> listSetProps = setProps.get(index);
            ImList<ImMap<?, K>> listMapSetProps = mapSetProps.get(index);
            int size = listSetProps.size();
            assert size == listMapSetProps.size();

            ImMap<X, T> map = (ImMap<X, T>) listSetProps.get(listIndex);
            if(listIndex == 0)
                listMapped = new boolean[size];

            for(int i=0;i<size;i++)
                if(!listMapped[i]) {
                    ImMap<X, K> mapMap = (ImMap<X, K>) listMapSetProps.get(i);
                    Set<T> addedKeys = new HashSet<>();

                    boolean incorrectMapping = false;
                    for(int j=0,sizeJ=map.size();j<sizeJ;j++) {
                        X propInt = map.getKey(j);
                        T mapInt = map.getValue(j);
                        K mapMapInt = mapMap.get(propInt);

                        K prevMap = mappedKeys.get(mapInt);
                        if(prevMap == null) {
                            mappedKeys.put(mapInt, mapMapInt);
                            addedKeys.add(mapInt);
                        } else {
                            if(!BaseUtils.hashEquals(mapMapInt, prevMap)) { // wrong mapping
                                incorrectMapping = true;
                                break;
                            }
                        }

                    }

                    if(!incorrectMapping) {
                        listMapped[i] = true;

                        boolean last = listIndex == size - 1;
                        if(last && index == setProps.size() - 1)
                            return true;
                        if(map(setProps, mapSetProps, last ? index + 1 : index, last ? 0 : listIndex + 1, last ? null : listMapped, mappedKeys))
                            return true;

                        listMapped[i] = false;
                    }
                    BaseUtils.removeKeys(mappedKeys, addedKeys);
                }

            return false;
        }
    }

    public static abstract class CachedFactory {

        // in theory we can improve caching key, by adding some mapping params
        private final Map<ImList<ImMap<Property, Integer>>, List<CacheResult>> caches = new HashMap<>();

        protected <T, X> PropertyImplement<?, T> create(ImList<ImSet<PropertyImplement<?, T>>> propertyImplements) {

            // ordering and splitting to 2 lists
            int size = propertyImplements.size();
            MList<ImMap<Property, Integer>> mProperties = ListFact.mList(size);
            MList<ImList<ImMap<?, T>>> mMappings = ListFact.mList();
            for(int i=0;i<size;i++) {
                ImMap<Property, ImSet<PropertyImplement<?, T>>> group = propertyImplements.get(i).group(key -> key.property);

                // calculating property counts
                mProperties.add(group.mapValues(ImCol::size));

                // sorting all properties and adding mappings to the global mappings list
                mMappings.addAll(group.sort(BusinessLogics.propComparator()).valuesList().mapListValues(pimps -> pimps.toList().mapListValues(pi -> pi.mapping)));
            }
            ImList<ImMap<Property, Integer>> properties = mProperties.immutableList();
            ImList<ImList<ImMap<?, T>>> mappings = mMappings.immutableList();

            List<CacheResult> cacheResults;
            synchronized (caches) {
                cacheResults = caches.computeIfAbsent(properties, k -> new ArrayList<>());
            }
            synchronized (cacheResults) {
                PropertyImplement<?, T> result;
                for (CacheResult<X> cacheResult : cacheResults) {
                    result = cacheResult.map(mappings);
                    if (result != null)
                        return result;
                }

                result = createNotCached(propertyImplements);
                cacheResults.add(new CacheResult<>(mappings, result));
                return result;
            }
        }

        protected abstract <T, X extends PropertyInterface> PropertyImplement<?, T> createNotCached(ImList<ImSet<PropertyImplement<?, T>>> propertyImplements);
    }

    // converter to MapImplement
    private static class ConverterToMapImplement<X extends PropertyInterface, T> {

        private final MExclMap<X, T> mapping = MapFact.mExclMap();
        private final MAddExclMap<T, List<X>> revMapping = MapFact.mAddExclMap();

        public <P extends PropertyInterface> PropertyMapImplement<P, X> convert(PropertyImplement<P, T> implement) {

            ImRevValueMap<P, X> mMap = implement.mapping.mapItRevValues();
            MAddMap<T, Integer> mCount = MapFact.mAddOverrideMap();
            for(int i=0,size=implement.mapping.size();i<size;i++) {
                T mapInterface = implement.mapping.getValue(i);

                Integer currentCount;
                List<X> list = revMapping.get(mapInterface);
                if(list == null) {
                    list = new ArrayList<>();
                    revMapping.exclAdd(mapInterface, list);
                    currentCount = 0;
                } else {
                    currentCount = mCount.get(mapInterface);
                    if(currentCount == null)
                        currentCount = 0;
                }
                mCount.add(mapInterface, currentCount + 1);

                X interf;
                if(currentCount >= list.size()) {
                    interf = (X) new PropertyInterface();
                    list.add(interf);
                    mapping.exclAdd(interf, mapInterface);
                } else
                    interf = list.get(currentCount);

                mMap.mapValue(i, interf);
            }
            return new PropertyMapImplement<>(implement.property, mMap.immutableValueRev());
        }

        public ImSet<PropertyMapImplement<?, X>> convert(ImSet<PropertyImplement<?, T>> propImplements) {
            MExclSet<PropertyMapImplement<?, X>> mResult = SetFact.mExclSet(propImplements.size()); // side effects
            for(PropertyImplement<?, T> propImplement : propImplements)
                mResult.exclAdd(convert(propImplement));
            return mResult.immutable();
        }

        public ImMap<X, T> getMapping() {
            return mapping.immutable();
        }
    }

    public static class AndCachedFactory extends CachedFactory {

        public <T> PropertyImplement<?, T> create(ImSet<PropertyImplement<?, T>> propertyImplements) {
            return create(ListFact.singleton(propertyImplements));
        }

        @Override
        protected <T, X extends PropertyInterface> PropertyImplement<?, T> createNotCached(ImList<ImSet<PropertyImplement<?, T>>> propertyImplements) {
            ImSet<PropertyImplement<?, T>> ops = propertyImplements.single();

            ConverterToMapImplement<X, T> converter = new ConverterToMapImplement<>();

            ImSet<PropertyMapImplement<?, X>> ands = converter.convert(ops);
            ImMap<X, T> mapping = converter.getMapping();

            return createAnd(mapping.keys(), ands).mapImplement(mapping);
        }
    }

    public static class OrCachedFactory extends CachedFactory {

        public <T> PropertyImplement<?, T> create(ImSet<PropertyImplement<?, T>> propertyImplements) {
            return create(ListFact.singleton(propertyImplements));
        }

        @Override
        protected <T, X extends PropertyInterface> PropertyImplement<?, T> createNotCached(ImList<ImSet<PropertyImplement<?, T>>> propertyImplements) {
            ImSet<PropertyImplement<?, T>> ops = propertyImplements.single();

            ConverterToMapImplement<X, T> converter = new ConverterToMapImplement<>();

            ImSet<PropertyMapImplement<?, X>> ors = converter.convert(ops).mapSetValues(value -> {
                if (!value.property.getType().equals(LogicalClass.instance)) // converting to logical if needed
                    return PropertyFact.createNotNull(value);
                return value;
            });
            ImMap<X, T> mapping = converter.getMapping();

            return createUnion(mapping.keys(), ors.toList()).mapImplement(mapping);
        }
    }

    public static class CompareCachedFactory extends CachedFactory {

        private final Compare compare;

        public CompareCachedFactory(Compare compare) {
            this.compare = compare;
        }

        public <T> PropertyImplement<?, T> create(PropertyImplement<?, T> propertyA, PropertyImplement<?, T> propertyB) {
            return create(ListFact.toList(SetFact.singleton(propertyA), SetFact.singleton(propertyB)));
        }

        @Override
        protected <T, X extends PropertyInterface> PropertyImplement<?, T> createNotCached(ImList<ImSet<PropertyImplement<?, T>>> propertyImplements) {

            ConverterToMapImplement<X, T> converter = new ConverterToMapImplement<>();
            PropertyMapImplement<?, X> propertyA = converter.convert(propertyImplements.get(0).single());
            PropertyMapImplement<?, X> propertyB = converter.convert(propertyImplements.get(1).single());
            ImMap<X, T> mapping = converter.getMapping();

            return createCompare(mapping.keys(), propertyA, propertyB, compare).mapImplement(mapping);
        }
    }

    public static class NotCachedFactory extends CachedFactory {

        public <T> PropertyImplement<?, T> create(PropertyImplement<?, T> property) {
            return create(ListFact.singleton(SetFact.singleton(property)));
        }

        @Override
        protected <T, X extends PropertyInterface> PropertyImplement<?, T> createNotCached(ImList<ImSet<PropertyImplement<?, T>>> propertyImplements) {

            ConverterToMapImplement<X, T> converter = new ConverterToMapImplement<>();
            PropertyMapImplement<?, X> property = converter.convert(propertyImplements.single().single());
            ImMap<X, T> mapping = converter.getMapping();

            return createNot(mapping.keys(), property).mapImplement(mapping);
        }
    }

    public static class IfCachedFactory extends CachedFactory {

        public IfCachedFactory() {
        }

        public <T> PropertyImplement<?, T> create(PropertyImplement<?, T> propertyA, PropertyImplement<?, T> propertyB) {
            return create(ListFact.toList(SetFact.singleton(propertyA), SetFact.singleton(propertyB)));
        }

        @Override
        protected <T, X extends PropertyInterface> PropertyImplement<?, T> createNotCached(ImList<ImSet<PropertyImplement<?, T>>> propertyImplements) {

            ConverterToMapImplement<X, T> converter = new ConverterToMapImplement<>();
            PropertyMapImplement<?, X> propertyA = converter.convert(propertyImplements.get(0).single());
            PropertyMapImplement<?, X> propertyB = converter.convert(propertyImplements.get(1).single());
            ImMap<X, T> mapping = converter.getMapping();

            return PropertyFact.createAnd(mapping.keys(), propertyA, propertyB).mapImplement(mapping);
        }
    }

    private static final OrCachedFactory orCachedFactory = new OrCachedFactory();
    public static <T> PropertyImplement<?, T> createOrCached(ImSet<PropertyImplement<?, T>> propertyImplements) {
        return orCachedFactory.create(propertyImplements);
    }

    private static final AndCachedFactory andCachedFactory = new AndCachedFactory();
    public static <T> PropertyImplement<?, T> createAndCached(ImSet<PropertyImplement<?, T>> propertyImplements) {
        return andCachedFactory.create(propertyImplements);
    }

    private static final Map<Compare, CompareCachedFactory> compareCachedFactories;
    static {
        compareCachedFactories = new HashMap<>();
        for(Compare compare : Compare.values())
            compareCachedFactories.put(compare, new CompareCachedFactory(compare));
    }
    public static <T> PropertyImplement<?, T> createCompareCached(PropertyImplement<?, T> propertyA, Compare compare, PropertyImplement<?, T> propertyB) {
        return compareCachedFactories.get(compare).create(propertyA, propertyB);
    }

    private static final NotCachedFactory notCachedFactory = new NotCachedFactory();
    public static <T> PropertyImplement<?, T> createNotCached(PropertyImplement<?, T> property) {
        return notCachedFactory.create(property);
    }

    private static final IfCachedFactory ifCachedFactory = new IfCachedFactory();
    public static <T> PropertyImplement<?, T> createIfCached(PropertyImplement<?, T> property, PropertyImplement<?, T> and) {
        return ifCachedFactory.create(property, and);
    }

    private static final Object valueLock = new Object();
    private static Property valueProperty;
    public static <T> PropertyImplement<?, T> createValueCached(T mapping) {
        synchronized (valueLock) {
            if(valueProperty == null)
                valueProperty = new AndFormulaProperty(0);
            return valueProperty.getSingleImplement(mapping);
        }
    }
}
