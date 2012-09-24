package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.QuickSet;
import platform.interop.Compare;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.ChangeClassActionProperty;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.PropertySet;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;
import static platform.server.logics.property.derived.DerivedProperty.*;

public class ForActionProperty<I extends PropertyInterface> extends ExtendContextActionProperty<I> {
   
    private final CalcPropertyMapImplement<?, I> ifProp; // calculate
    private final OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> orders; // calculate
    private final boolean ordersNotNull;
    private final ActionPropertyMapImplement<?, I> action; // action
    private final ActionPropertyMapImplement<?, I> elseAction; // action
    private final boolean recursive;

    private final I addObject;
    private final CustomClass addClass;
    private final boolean forceDialog;
   
    public ForActionProperty(String sID, String caption, Collection<I> innerInterfaces, List<I> mapInterfaces, CalcPropertyMapImplement<?, I> ifProp, OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, I> action, ActionPropertyMapImplement<?, I> elseAction, I addObject, CustomClass addClass, boolean forceDialog, boolean recursive) {
       super(sID, caption, innerInterfaces, mapInterfaces);

        assert !recursive || (addObject == null && elseAction == null);

        this.ifProp = ifProp;
        this.orders = orders;
        this.ordersNotNull = ordersNotNull;
        this.action = action;
        this.elseAction = elseAction;
        this.recursive = recursive;

        this.addObject = addObject;
        this.addClass = addClass;
        this.forceDialog = forceDialog;

        finalizeInit();
        assert innerInterfaces.containsAll(merge(ifProp.mapping.values(), action.mapping.values()));
    }

    public Set<ActionProperty> getDependActions() {
       Set<ActionProperty> result = Collections.singleton((ActionProperty) action.property);
       if(elseAction != null)
           result = addSet(result, elseAction.property);
       return result;
    }

    @Override
    public Set<CalcProperty> getUsedProps() {
       Set<CalcProperty> result = new HashSet<CalcProperty>();
       ifProp.mapFillDepends(result);
       for(CalcPropertyInterfaceImplement<I> order : orders.keySet())
           order.mapFillDepends(result);
       result.addAll(super.getUsedProps());
       return result;
    }

    @Override
    protected FlowResult executeExtend(ExecutionContext<PropertyInterface> context, Map<I, KeyExpr> innerKeys, Map<I, DataObject> innerValues, Map<I, Expr> innerExprs) throws SQLException {
        FlowResult result = FlowResult.FINISH;

        boolean execElse = elseAction != null;
        
        assert recursive || addObject==null;

        Collection<Map<I, DataObject>> rows;
        RECURSIVE:
        do {
            rows = readRows(context, innerKeys, innerExprs);
            if (!rows.isEmpty()) {
                execElse = false;
            }
            for (Map<I, DataObject> row : rows) {
                FlowResult actionResult = execute(context, action, merge(innerValues, row), mapInterfaces);
                if (actionResult != FlowResult.FINISH) {
                    if (actionResult != FlowResult.BREAK) {
                        result = actionResult;
                    }
                    break RECURSIVE;
                }
            }
        } while (recursive && !rows.isEmpty());

        if (execElse) {
            return execute(context, elseAction, innerValues, mapInterfaces);
        }

        return result;
    }

    private Collection<Map<I, DataObject>> readRows(ExecutionContext<PropertyInterface> context, Map<I, KeyExpr> innerKeys, Map<I, Expr> innerExprs) throws SQLException {
        OrderedMap<Expr, Boolean> orderExprs = new OrderedMap<Expr, Boolean>();
        for (Map.Entry<CalcPropertyInterfaceImplement<I>, Boolean> order : orders.entrySet())
           orderExprs.put(order.getKey().mapExpr(innerExprs, context.getModifier()), order.getValue());

        return new PropertySet<I>(innerKeys, ifProp.mapExpr(innerExprs, context.getModifier()).getWhere(), orderExprs, ordersNotNull).executeClasses(context.getEnv());
    }

    protected CalcPropertyMapImplement<?, I> getGroupWhereProperty() {
       CalcPropertyMapImplement<?, I> whereProp = ifProp;
       if(ordersNotNull)
           whereProp = DerivedProperty.createAnd(innerInterfaces, ifProp, orders.keySet());
       return DerivedProperty.createIfElseUProp(innerInterfaces, whereProp,
               action.mapWhereProperty(), elseAction != null ? elseAction.mapWhereProperty() : null, false);
    }

    private Map<I, ValueClass> getExtendClasses() {
        return removeKeys(ifProp.mapInterfaceClasses(), mapInterfaces.values());
    }

    @Override
    public ActionPropertyMapImplement<?, I> compileExtend() { // проталкивание FOR'ов

        if (addObject != null) { // "компиляция" ADDOBJ
            assert !recursive;
            // сначала проверим если первый в списке CHANGE CLASS, тогда заберем его в FOR
            List<ActionPropertyMapImplement<?, I>> list = action.getList();

            if(list.size() > 0) {
                ActionPropertyMapImplement<?, I> first = list.get(0);
                if (first.mapping.size() == 1 && singleValue(first.mapping).equals(addObject) && first.property instanceof ChangeClassActionProperty) {
                    ChangeClassActionProperty changeClassProperty = (ChangeClassActionProperty) first.property;
                    if (changeClassProperty.valueClass instanceof CustomClass && changeClassProperty.where == null) // удаление не интересует
                        return DerivedProperty.createForAction(innerInterfaces, mapInterfaces.values(), ifProp, orders, ordersNotNull,
                                DerivedProperty.createListAction(innerInterfaces, list.subList(1, list.size())), elseAction, addObject,
                                (CustomClass) changeClassProperty.valueClass, changeClassProperty.forceDialog, recursive);
                }
            }

            CalcPropertyMapImplement<?, I> result = DerivedProperty.createDataProp(true, getExtendClasses(), addClass);
            return DerivedProperty.createListAction(mapInterfaces.values(), BaseUtils.<ActionPropertyMapImplement<?, I>>toList(
                    DerivedProperty.createAddAction(addClass, forceDialog, remove(innerInterfaces, addObject), mapInterfaces.values(), ifProp, result),
                    DerivedProperty.createForAction(innerInterfaces, mapInterfaces.values(), DerivedProperty.<I>createCompare(
                            addObject, result, Compare.EQUALS), orders, ordersNotNull, action, elseAction, null, null, false)));
        } else { // проталкиваем for'ы
            if (action.hasFlow(ChangeFlowType.BREAK, ChangeFlowType.APPLY, ChangeFlowType.CANCEL, ChangeFlowType.VOLATILE))
                return null;

            List<ActionPropertyMapImplement<?, I>> list = action.getList();

            QuickSet<CalcProperty>[] listChangeProps = new QuickSet[list.size()];
            Set<CalcProperty>[] listUsedProps = new Set[list.size()];
            for (int i = 0; i < list.size(); i++) {
                listChangeProps[i] = list.get(i).property.getChangeProps();
                listUsedProps[i] = list.get(i).property.getUsedProps();
            }

            // ищем сначала "вытаскиваемые" (changeProps не зависят от usedProps и т.д)
            final QuickSet<CalcProperty> pushChangedProps = new QuickSet<CalcProperty>();
            List<ActionPropertyMapImplement<?, I>> canBePushed = new ArrayList<ActionPropertyMapImplement<?, I>>();
            List<ActionPropertyMapImplement<?, I>> rest = new ArrayList<ActionPropertyMapImplement<?, I>>();
            for (int i = 0; i < list.size(); i++) {
                ActionPropertyMapImplement<?, I> itemAction = list.get(i);

                if (itemAction.hasPushFor(mapInterfaces.values(), ordersNotNull)) {
                    QuickSet<CalcProperty> siblingChangeProps = new QuickSet<CalcProperty>();
                    Set<CalcProperty> siblingUsedProps = new HashSet<CalcProperty>();
                    for (int j = 0; j < list.size(); j++) // читаем sibling'и
                        if (j != i) {
                            siblingChangeProps.addAll(listChangeProps[j]);
                            siblingUsedProps.addAll(listUsedProps[j]);
                        }

                    QuickSet<CalcProperty> changeProps = listChangeProps[i];
                    Set<CalcProperty> usedProps = listUsedProps[i];

                    CalcProperty where = itemAction.getPushWhere(mapInterfaces.values(), ordersNotNull);
                    if (!CalcProperty.depends(siblingUsedProps, changeProps) && // не меняют сиблингов
                            !CalcProperty.depends(usedProps, siblingChangeProps) && // сиблинги не меняют
                            !CalcProperty.depends(where!=null?Collections.singleton(where):usedProps, changeProps) && // не рекурсивно зависимо
                            siblingChangeProps.disjoint(changeProps)) { // несколько раз не меняется
                        canBePushed.add(itemAction);
                        pushChangedProps.addAll(changeProps);
                    } else
                        rest.add(itemAction);
                } else
                    rest.add(itemAction);
            }

            if (canBePushed.size() == 0)
                return null;

            List<ActionPropertyMapImplement<?, I>> result = new ArrayList<ActionPropertyMapImplement<?, I>>();

            CalcPropertyMapImplement<?, I> pushProp = ifProp;
            if ((canBePushed.size() + (rest.size() > 0 ? 1 : 0) > 1)) {// если кол-во(вытаскиваемые+оставшиеся) > 1
                if (CalcProperty.dependsImplement(orders.keySet(), pushChangedProps)) // если orders'ы меняются пока не проталкиваем
                    return null;

                if (CalcProperty.depends(ifProp.property, pushChangedProps) || // если есть свойства из базы или меняет условия
                        CalcProperty.depends(ifProp.property, StoredDataProperty.set)) {
                    pushProp = DerivedProperty.createDataProp(true, getExtendClasses(), ifProp.property.getValueClass()); // делаем SET в session свойство, и подменяем условие на это свойство
                    result.add(DerivedProperty.createSetAction(innerInterfaces, mapInterfaces.values(), null, pushProp, ifProp));
                }
            }

            // "вытаскиваемым" проталкиваем where + order и добавляем в начало
            for (ActionPropertyMapImplement<?, I> property : canBePushed)
                result.add(property.pushFor(mapInterfaces.values(), pushProp, orders, ordersNotNull));

            // добавляем оставшиеся, с for'ом компилируя внутренние элементы
            if (rest.size() > 0)
                result.add(DerivedProperty.createForAction(innerInterfaces, mapInterfaces.values(), pushProp, orders,
                        ordersNotNull, DerivedProperty.createListAction(innerInterfaces, rest), elseAction, false));

            return DerivedProperty.createListAction(mapInterfaces.values(), result);
        }
    }

    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(Map<PropertyInterface, T> mapping, Collection<T> context, boolean ordersNotNull) {
        return elseAction==null && !hasFlow(ChangeFlowType.BREAK) && ForActionProperty.this.ordersNotNull == ordersNotNull && elseAction == null && !recursive; // потом отработаем эти случаи
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> CalcProperty getPushWhere(Map<PropertyInterface, T> mapping, Collection<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        return ifProp.property; // тут не null должен возвращаться
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?, T> pushFor(Map<PropertyInterface, T> mapping, Collection<T> context, CalcPropertyMapImplement<PW, T> push, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        return pushFor(innerInterfaces, ifProp, mapInterfaces, mapping, context, push, orders, ordersNotNull, new PushFor<I, PropertyInterface>() {
            public ActionPropertyMapImplement<?, PropertyInterface> push(Collection<PropertyInterface> context, CalcPropertyMapImplement<?, PropertyInterface> where, OrderedMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders, boolean ordersNotNull, Map<I, PropertyInterface> mapInnerInterfaces) {
                return createForAction(context, where, BaseUtils.mergeOrders(orders, mapImplements(ForActionProperty.this.orders, mapInnerInterfaces)), ordersNotNull, action.map(mapInnerInterfaces), null, addObject != null ? mapInnerInterfaces.get(addObject): null, addClass, forceDialog, false);
            }
        });
    }

//        (e, x)
//              FOR F(a,c,d,x) - внешн. (e,x) внутр. (a,c,d)
//                    FOR/IF/WHERE h(a,b,c,e) - внешн. (a,c,e) внутр. (b)
//                      X(a,b,c,e);
//        TO
//        (e, x)
//              FOR F(a,c,d,x) AND h(a,b,c,e) - внешн. (e,x)
//                  X(a,b,c,e);


    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type != ChangeFlowType.BREAK && super.hasFlow(type);
    }

    public static interface PushFor<PI extends PropertyInterface, I extends PropertyInterface> {
        ActionPropertyMapImplement<?, I> push(Collection<I> context, CalcPropertyMapImplement<?, I> where, OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, Map<PI, I> mapInnerInterfaces);
    }

    public static <T extends PropertyInterface, I extends PropertyInterface, W extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?, T> pushFor(
            Collection<I> innerInterfaces, CalcPropertyMapImplement<W, I> forProp, Map<PropertyInterface, I> mapInterfaces, Map<PropertyInterface, T> mapping, Collection<T> context, CalcPropertyMapImplement<PW, T> push,
            OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull, PushFor<I, PropertyInterface> pushFor) {
        assert !ordersNotNull; // в противном случае придется еще с orders'ов собирать интерфейсы
        assert mapInterfaces.keySet().equals(mapping.keySet());

        // сначала and'им where и push, получаем интерфейсы I + push (T)
        Map<T, PropertyInterface> mapPushInterfaces = new HashMap<T, PropertyInterface>(); Map<I, PropertyInterface> mapInnerInterfaces = new HashMap<I, PropertyInterface>();
        createCommon(mergeColSet(mapping.values(), push.mapping.values()), innerInterfaces, crossJoin(mapping, mapInterfaces), mapPushInterfaces, mapInnerInterfaces);

        CalcPropertyMapImplement<?, PropertyInterface> mapPush = push.map(mapPushInterfaces);
        if(forProp!=null)
            mapPush = createAnd(mapPush, forProp.map(mapInnerInterfaces));

        return pushFor.push(BaseUtils.filterKeys(mapPushInterfaces, context).values(), mapPush,
                DerivedProperty.mapImplements(orders, mapPushInterfaces), ordersNotNull, mapInnerInterfaces).map(reverse(mapPushInterfaces));
    }
}
