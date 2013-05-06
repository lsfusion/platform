package platform.server.logics.property.actions.flow;

import platform.base.Result;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MList;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.interop.Compare;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.ChangeClassActionProperty;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.PropertyChange;
import platform.server.session.PropertySet;

import java.sql.SQLException;
import java.util.Collections;

import static platform.server.logics.property.derived.DerivedProperty.*;

public class ForActionProperty<I extends PropertyInterface> extends ExtendContextActionProperty<I> {
   
    private final CalcPropertyMapImplement<?, I> ifProp; // calculate
    private final ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders; // calculate
    private final boolean ordersNotNull;
    private final ActionPropertyMapImplement<?, I> action; // action
    private final ActionPropertyMapImplement<?, I> elseAction; // action
    private final boolean recursive;

    private final I addObject;
    private final CustomClass addClass;
    private final boolean forceDialog;

    private final ImSet<I> noInline; // из extend interfaces
    private final boolean forceInline;
   
    public ForActionProperty(String sID, String caption, ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces, CalcPropertyMapImplement<?, I> ifProp, ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, I> action, ActionPropertyMapImplement<?, I> elseAction, I addObject, CustomClass addClass, boolean forceDialog, boolean recursive, ImSet<I> noInline, boolean forceInline) {
       super(sID, caption, innerInterfaces, mapInterfaces);

        assert !recursive || (addObject == null && elseAction == null);
        assert !(addObject == null && ifProp == null);

        this.ifProp = ifProp;
        this.orders = orders;
        this.ordersNotNull = ordersNotNull;
        this.action = action;
        this.elseAction = elseAction;
        this.recursive = recursive;

        this.addObject = addObject;
        this.addClass = addClass;
        this.forceDialog = forceDialog;

        this.noInline = noInline;
        this.forceInline = forceInline;

        assert (addObject==null || !noInline.contains(addObject)) && !noInline.intersect(mapInterfaces.getSet()) && innerInterfaces.containsAll(noInline);

        finalizeInit();
        assert innerInterfaces.containsAll(action.mapping.valuesSet().merge(ifProp != null ? ifProp.mapping.valuesSet() : SetFact.<I>EMPTY()));
    }

    public ImSet<ActionProperty> getDependActions() {
       ImSet<ActionProperty> result = SetFact.singleton((ActionProperty) action.property);
       if(elseAction != null)
           result = result.merge(elseAction.property);
       return result;
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
       MSet<CalcProperty> mUsed = SetFact.mSet();
       if(ifProp!=null)
           ifProp.mapFillDepends(mUsed);
       for(CalcPropertyInterfaceImplement<I> order : orders.keyIt())
           order.mapFillDepends(mUsed);
       return mUsed.immutable().toMap(false).merge(super.aspectUsedExtProps(), addValue);
    }

    @Override
    protected FlowResult executeExtend(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, DataObject> innerValues, ImMap<I, Expr> innerExprs) throws SQLException {
        FlowResult result = FlowResult.FINISH;

        boolean execElse = elseAction != null;

        assert recursive || addObject==null;

        ImOrderSet<ImMap<I, DataObject>> rows;
        RECURSIVE:
        do {
            rows = readRows(context, innerKeys, innerExprs);
            if (!rows.isEmpty()) {
                execElse = false;
            }
            for (ImMap<I, DataObject> row : rows) {
                FlowResult actionResult = execute(context, action, innerValues.addExcl(row), mapInterfaces);
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

    private ImOrderSet<ImMap<I, DataObject>> readRows(final ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends Expr> innerExprs) throws SQLException {
        assert ifProp!=null; // так как предполагается компайлится
        Where where = ifProp.mapExpr(innerExprs, context.getModifier()).getWhere();

        final ImMap<I, ? extends Expr> fInnerExprs = PropertyChange.simplifyExprs(innerExprs, where);
        ImOrderMap<Expr, Boolean> orderExprs = orders.mapMergeOrderKeys(new GetValue<Expr, CalcPropertyInterfaceImplement<I>>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<I> value) {
                return value.mapExpr(fInnerExprs, context.getModifier());
            }});

        return new PropertySet<I>(innerKeys, where, orderExprs, ordersNotNull).executeClasses(context.getEnv());
    }

    protected CalcPropertyMapImplement<?, I> getGroupWhereProperty() {
       CalcPropertyMapImplement<?, I> whereProp = ifProp != null ? ifProp : DerivedProperty.<I>createTrue();
       if(ordersNotNull)
           whereProp = DerivedProperty.createAnd(innerInterfaces, whereProp, orders.keys());
       return DerivedProperty.createIfElseUProp(innerInterfaces, whereProp,
               action.mapWhereProperty(), elseAction != null ? elseAction.mapWhereProperty() : null);
    }

    private ImMap<I, ValueClass> getExtendClasses() {
        if(ifProp==null)
            return MapFact.<I, ValueClass>EMPTY();
        return ifProp.mapInterfaceClasses().remove(mapInterfaces.valuesSet());
    }

    @Override
    public ActionPropertyMapImplement<?, I> compileExtend() { // проталкивание FOR'ов

        ImSet<I> context = mapInterfaces.valuesSet();
        assert recursive || innerInterfaces.size() > context.size();
        boolean allNoInline = !recursive && (innerInterfaces.size() == context.size() + noInline.size() + (addObject !=null ? 1 : 0));

        if(!allNoInline && noInline.size() > 0) {
            assert !noInline.intersect(context);
            assert orders.isEmpty();
            assert !recursive;

            MList<ActionPropertyMapImplement<?, I>> mResult = ListFact.mList();
            ImSet<I> extNoInline = context.addExcl(noInline);

            CalcPropertyMapImplement<?, I> noInlineIfProp = ifProp;
            ImSet<I> noInlineInterfaces = extNoInline;
            if(CalcProperty.depends(ifProp.property, StoredDataProperty.set)) { // нужно создать сначала материалайзить условие for по аналогии с проталкиванием
                noInlineIfProp = DerivedProperty.createDataProp(true, getExtendClasses(), ifProp.property.getValueClass());// делаем SET в session свойство, и подменяем условие на это свойство
                mResult.add(DerivedProperty.createSetAction(innerInterfaces, context, null, noInlineIfProp, ifProp));
                noInlineInterfaces = noInline;
            }

            // затем сделать GROUP ANY TRUE IF с группировкой по noInline интерфейсам, затем
            CalcPropertyMapImplement<?, I> groupNoInline = DerivedProperty.createAnyGProp(noInlineIfProp, noInlineInterfaces);
            // по нему уже сгруппировать FOR noInline интерфейсам с опцией Inline.NO, а внутри FOR по материализованному условию где noInline уже будут внешними интерфейсами
            ActionPropertyMapImplement<?, I> cleanAction = createForAction(innerInterfaces, extNoInline, noInlineIfProp, MapFact.<CalcPropertyInterfaceImplement<I>, Boolean>EMPTYORDER(), false,
                    action, null, addObject, addClass, forceDialog, recursive, SetFact.<I>EMPTY(), forceInline);
            mResult.add(createForAction(extNoInline, context, groupNoInline, MapFact.<CalcPropertyInterfaceImplement<I>, Boolean>EMPTYORDER(), false,
                    cleanAction, elseAction, false, noInline, false));
            return DerivedProperty.createListAction(context, mResult.immutableList());
        }

        if (addObject != null) { // "компиляция" ADDOBJ
            assert !recursive;
            // сначала проверим если первый в списке CHANGE CLASS, тогда заберем его в FOR
            ImList<ActionPropertyMapImplement<?, I>> list = action.getList();

            if(list.size() > 0) {
                ActionPropertyMapImplement<?, I> first = list.get(0);
                if (first.mapping.size() == 1 && first.mapping.singleValue().equals(addObject) && first.property instanceof ChangeClassActionProperty) {
                    ChangeClassActionProperty changeClassProperty = (ChangeClassActionProperty) first.property;
                    if (changeClassProperty.valueClass instanceof CustomClass && changeClassProperty.where == null) // удаление не интересует
                        return DerivedProperty.createForAction(innerInterfaces, context, ifProp, orders, ordersNotNull,
                                DerivedProperty.createListAction(innerInterfaces, list.subList(1, list.size())), elseAction, addObject,
                                (CustomClass) changeClassProperty.valueClass, changeClassProperty.forceDialog, recursive, noInline, forceInline);
                }
            }

            CalcPropertyMapImplement<?, I> result = DerivedProperty.createDataProp(true, getExtendClasses(), addClass);
            return DerivedProperty.createListAction(context, ListFact.<ActionPropertyMapImplement<?, I>>toList(
                    DerivedProperty.createAddAction(addClass, forceDialog, innerInterfaces.removeIncl(addObject), context, ifProp, result),
                    DerivedProperty.createForAction(innerInterfaces, context, DerivedProperty.<I>createCompare(
                            addObject, result, Compare.EQUALS), orders, ordersNotNull, action, elseAction, null, null, false, noInline, forceInline)));
        }

        if(allNoInline)
            return null;

        // проталкиваем for'ы
        if (action.hasFlow(ChangeFlowType.BREAK, ChangeFlowType.APPLY, ChangeFlowType.CANCEL, ChangeFlowType.VOLATILE))
            return null;

        ImList<ActionPropertyMapImplement<?, I>> list = action.getList();

        ImSet<CalcProperty>[] listChangeProps = new ImSet[list.size()];
        ImSet<CalcProperty>[] listUsedProps = new ImSet[list.size()];
        for (int i = 0; i < list.size(); i++) {
            listChangeProps[i] = list.get(i).property.getChangeProps();
            listUsedProps[i] = list.get(i).property.getUsedProps();
        }

        // ищем сначала "вытаскиваемые" (changeProps не зависят от usedProps и т.д)
        final MSet<CalcProperty> mPushChangedProps = SetFact.mSet();
        MList<ActionPropertyMapImplement<?, I>> mCanBePushed = ListFact.mFilter(list);
        MList<ActionPropertyMapImplement<?, I>> mRest = ListFact.mFilter(list);
        for (int i = 0; i < list.size(); i++) {
            ActionPropertyMapImplement<?, I> itemAction = list.get(i);

            if (itemAction.hasPushFor(context, ordersNotNull)) {
                MSet<CalcProperty> mSiblingChangeProps = SetFact.mSet();
                MSet<CalcProperty> mSiblingUsedProps = SetFact.mSet();
                for (int j = 0; j < list.size(); j++) // читаем sibling'и
                    if (j != i) {
                        mSiblingChangeProps.addAll(listChangeProps[j]);
                        mSiblingUsedProps.addAll(listUsedProps[j]);
                    }
                ImSet<CalcProperty> siblingChangeProps = mSiblingChangeProps.immutable();
                ImSet<CalcProperty> siblingUsedProps = mSiblingUsedProps.immutable();

                ImSet<CalcProperty> changeProps = listChangeProps[i];
                ImSet<CalcProperty> usedProps = listUsedProps[i];

                CalcProperty where = itemAction.getPushWhere(context, ordersNotNull);
                if (forceInline || (!CalcProperty.depends(siblingUsedProps, changeProps) && // не меняют сиблингов
                        !CalcProperty.depends(usedProps, siblingChangeProps) && // сиблинги не меняют
                        !CalcProperty.depends(where!=null?Collections.singleton(where):usedProps, changeProps) && // не рекурсивно зависимо
                        siblingChangeProps.disjoint(changeProps))) { // несколько раз не меняется
                    mCanBePushed.add(itemAction);
                    mPushChangedProps.addAll(changeProps);
                } else
                    mRest.add(itemAction);
            } else
                mRest.add(itemAction);
        }
        ImSet<CalcProperty> pushChangedProps = mPushChangedProps.immutable();
        ImList<ActionPropertyMapImplement<?, I>> canBePushed = ListFact.imFilter(mCanBePushed, list);
        ImList<ActionPropertyMapImplement<?, I>> rest = ListFact.imFilter(mRest, list);

        if (canBePushed.size() == 0)
            return null;

        MList<ActionPropertyMapImplement<?, I>> mResult = ListFact.mList();

        CalcPropertyMapImplement<?, I> pushProp = ifProp;
        if ((canBePushed.size() + (rest.size() > 0 ? 1 : 0) > 1)) {// если кол-во(вытаскиваемые+оставшиеся) > 1
            if (CalcProperty.dependsImplement(orders.keys(), pushChangedProps)) // если orders'ы меняются пока не проталкиваем
                return null;

            if (CalcProperty.depends(ifProp.property, pushChangedProps) || // если есть stored свойства (а не чисто session) или меняет условия
                    CalcProperty.depends(ifProp.property, StoredDataProperty.set)) {
                pushProp = DerivedProperty.createDataProp(true, getExtendClasses(), ifProp.property.getValueClass()); // делаем SET в session свойство, и подменяем условие на это свойство
                mResult.add(DerivedProperty.createSetAction(innerInterfaces, context, null, pushProp, ifProp));
            }
        }

        // "вытаскиваемым" проталкиваем where + order и добавляем в начало
        for (ActionPropertyMapImplement<?, I> property : canBePushed)
            mResult.add(property.pushFor(context, pushProp, orders, ordersNotNull));

        // добавляем оставшиеся, с for'ом компилируя внутренние элементы
        if (rest.size() > 0)
            mResult.add(DerivedProperty.createForAction(innerInterfaces, context, pushProp, orders,
                    ordersNotNull, DerivedProperty.createListAction(innerInterfaces, rest), elseAction, false, innerInterfaces.remove(context), false));

        return DerivedProperty.createListAction(context, mResult.immutableList());
    }

    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return elseAction==null && !hasFlow(ChangeFlowType.BREAK) && ForActionProperty.this.ordersNotNull == ordersNotNull && elseAction == null && !recursive; // потом отработаем эти случаи
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> CalcProperty getPushWhere(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        return ifProp !=null ? ifProp.property : DerivedProperty.createTrue().property; // тут не null должен возвращаться
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?, T> pushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, CalcPropertyMapImplement<PW, T> push, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        return pushFor(innerInterfaces, ifProp, mapInterfaces, mapping, context, push, orders, ordersNotNull, new PushFor<I, PropertyInterface>() {
            public ActionPropertyMapImplement<?, PropertyInterface> push(ImSet<PropertyInterface> context, CalcPropertyMapImplement<?, PropertyInterface> where, ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders, boolean ordersNotNull, ImRevMap<I, PropertyInterface> mapInnerInterfaces) {
                return createForAction(context, where, orders.mergeOrder(mapImplements(ForActionProperty.this.orders, mapInnerInterfaces)), ordersNotNull, action.map(mapInnerInterfaces), null, addObject != null ? mapInnerInterfaces.get(addObject): null, addClass, forceDialog, false, noInline.mapRev(mapInnerInterfaces), forceInline);
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
        ActionPropertyMapImplement<?, I> push(ImSet<I> context, CalcPropertyMapImplement<?, I> where, ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, ImRevMap<PI, I> mapInnerInterfaces);
    }

    public static <T extends PropertyInterface, I extends PropertyInterface, W extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?, T> pushFor(
            ImSet<I> innerInterfaces, CalcPropertyMapImplement<W, I> forProp, ImRevMap<PropertyInterface, I> mapInterfaces, ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, CalcPropertyMapImplement<PW, T> push,
            ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull, PushFor<I, PropertyInterface> pushFor) {
        assert !ordersNotNull; // в противном случае придется еще с orders'ов собирать интерфейсы
        assert mapInterfaces.keys().equals(mapping.keys());

        // сначала and'им where и push, получаем интерфейсы I + push (T)
        Result<ImRevMap<T, PropertyInterface>> mapPushInterfaces = new Result<ImRevMap<T, PropertyInterface>>(); Result<ImRevMap<I, PropertyInterface>> mapInnerInterfaces = new Result<ImRevMap<I, PropertyInterface>>();
        createCommon(mapping.valuesSet().merge(push.mapping.valuesSet()), innerInterfaces, mapping.crossJoin(mapInterfaces), mapPushInterfaces, mapInnerInterfaces);

        CalcPropertyMapImplement<?, PropertyInterface> mapPush = push.map(mapPushInterfaces.result);
        if(forProp!=null)
            mapPush = createAnd(mapPush, forProp.map(mapInnerInterfaces.result));

        return pushFor.push(mapPushInterfaces.result.filterRev(context).valuesSet(), mapPush,
                DerivedProperty.mapImplements(orders, mapPushInterfaces.result), ordersNotNull, mapInnerInterfaces.result).map(mapPushInterfaces.result.reverse());
    }
}
