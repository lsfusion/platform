package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetExValue;
import lsfusion.interop.Compare;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.LogTime;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.ChangeClassActionProperty;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.PropertySet;
import lsfusion.server.session.UpdateCurrentClasses;
import lsfusion.server.stack.ExecutionStackAspect;
import lsfusion.server.stack.ParamMessage;
import lsfusion.server.stack.ProgressStackItem;
import lsfusion.server.stack.ThisMessage;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;

import static lsfusion.server.logics.property.derived.DerivedProperty.*;

public class ForActionProperty<I extends PropertyInterface> extends ExtendContextActionProperty<I> {
   
    private final CalcPropertyMapImplement<?, I> ifProp; // calculate
    private final ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders; // calculate
    private final boolean ordersNotNull;
    private final ActionPropertyMapImplement<?, I> action; // action
    private final ActionPropertyMapImplement<?, I> elseAction; // action
    private final boolean recursive;

    private final I addObject;
    private final CustomClass addClass;

    private final ImSet<I> noInline; // из extend interfaces
    private final boolean forceInline;
   
    public ForActionProperty(LocalizedString caption, ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces, CalcPropertyMapImplement<?, I> ifProp, ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, I> action, ActionPropertyMapImplement<?, I> elseAction, I addObject, CustomClass addClass, boolean recursive, ImSet<I> noInline, boolean forceInline) {
       super(caption, innerInterfaces, mapInterfaces);

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
    
    private static class RowUpdateIterate<I extends PropertyInterface> implements Iterable<ImMap<I, DataObject>>, Iterator<ImMap<I, DataObject>>, UpdateCurrentClasses {
        private ImOrderSet<ImMap<I, DataObject>> rows;

        private RowUpdateIterate(ImOrderSet<ImMap<I, DataObject>> rows) {
            this.rows = rows;
        }

        public Iterator<ImMap<I, DataObject>> iterator() {
            return this;
        }

        public void updateOnApply(DataSession session) throws SQLException, SQLHandledException {
            final ImOrderSet<ImMap<I, DataObject>> prevRows = rows;
            session.addRollbackInfo(new Runnable() {
                public void run() {
                    rows = prevRows;
                }
            });
            MOrderExclSet<ImMap<I, DataObject>> mRows = SetFact.mOrderExclSet(rows.size() - i);
            for(int j=i;j<rows.size();j++)
                mRows.exclAdd(session.updateCurrentClasses(rows.get(j)));
            i=0;
            rows = mRows.immutableOrder();
        }

        int i = 0;
        public void remove() {
            throw new UnsupportedOperationException();
        }

        public ImMap<I, DataObject> next() {
            return rows.get(i++);
        }

        public boolean hasNext() {
            return i < rows.size(); 
        }
    }

    @Override
    protected FlowResult executeExtend(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends ObjectValue> innerValues, ImMap<I, Expr> innerExprs) throws SQLException, SQLHandledException {
        FlowResult result = FlowResult.FINISH;

        boolean execElse = elseAction != null;
        
        assert !isHackAdd();        
        if(addObject != null) {
            innerKeys = innerKeys.removeRev(addObject);
            innerExprs = innerExprs.remove(addObject);
        }

        ImOrderSet<ImMap<I, DataObject>> rows;
        RECURSIVE:
        do {
            rows = readRows(context, innerKeys, innerExprs);
            if (!rows.isEmpty()) {
                execElse = false;
            }
            RowUpdateIterate<I> rowUpdate = new RowUpdateIterate<>(rows);
            context.pushUpdate(rowUpdate);
            ProgressStackItem stackItem = ThreadLocalContext.pushProgressMessage(ExecutionStackAspect.getProgressBarLastActionString(), 0, rowUpdate.rows.size());
            try {
                for (int i = 0; i < rowUpdate.rows.size(); i++) {
                    ImMap<I, DataObject> row = rowUpdate.rows.get(i);
                    ImMap<I, ObjectValue> newValues = MapFact.addExcl(innerValues, row);
                    if(addObject!=null)
                        newValues = MapFact.addExcl(newValues, addObject, context.addObject((ConcreteCustomClass) addClass));

                    ThreadLocalContext.popActionMessage(stackItem);
                    stackItem = ThreadLocalContext.pushProgressMessage(ExecutionStackAspect.getProgressBarLastActionString(), i + 1, rowUpdate.rows.size());
                    FlowResult actionResult = executeFor(context, newValues);
                    if (actionResult != FlowResult.FINISH) {
                        if (actionResult != FlowResult.BREAK) {
                            result = actionResult;
                        }
                        break RECURSIVE;
                    }
                }
            } finally {
                context.popUpdate();
                ThreadLocalContext.popActionMessage(stackItem);
            }
        } while (recursive && !rows.isEmpty());

        if (execElse) {
            elseAction.map(mapInterfaces.reverse()).execute(context);
        }

        return result;
    }

    @LogTime
    @ThisMessage
    private FlowResult executeFor(ExecutionContext<PropertyInterface> context, @ParamMessage (profile = false) ImMap<I, ObjectValue> newValues) throws SQLException, SQLHandledException {
        return execute(context, action, newValues, mapInterfaces);
    }

    private static <P extends PropertyInterface, M extends  PropertyInterface> FlowResult execute(ExecutionContext<PropertyInterface> context, ActionPropertyMapImplement<P, M> implement, ImMap<M, ? extends ObjectValue> keys, ImRevMap<PropertyInterface, M> mapInterfaces) throws SQLException, SQLHandledException {
        return implement.property.execute(
                context.override(
                        implement.mapping.join(keys),
                        BaseUtils.<ImMap<P, CalcPropertyInterfaceImplement<PropertyInterface>>>immutableCast(
                                MapFact.innerCrossValues(implement.mapping, mapInterfaces)
                        )
                )
        );
    }

    private ImOrderSet<ImMap<I, DataObject>> readRows(final ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends Expr> innerExprs) throws SQLException, SQLHandledException {
        if(ifProp == null)
            return SetFact.singletonOrder(MapFact.<I, DataObject>EMPTY());
            
        Where where = ifProp.mapExpr(innerExprs, context.getModifier()).getWhere();

        final ImMap<I, ? extends Expr> fInnerExprs = PropertyChange.simplifyExprs(innerExprs, where);
        ImOrderMap<Expr, Boolean> orderExprs = orders.mapMergeOrderKeysEx(new GetExValue<Expr, CalcPropertyInterfaceImplement<I>, SQLException, SQLHandledException>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<I> value) throws SQLException, SQLHandledException {
                return value.mapExpr(fInnerExprs, context.getModifier());
            }
        });

        return new PropertySet<>(innerKeys, where, orderExprs, ordersNotNull).executeClasses(context.getEnv());
    }

    protected CalcPropertyMapImplement<?, I> calcGroupWhereProperty() {
       CalcPropertyMapImplement<?, I> whereProp = ifProp != null ? ifProp : DerivedProperty.<I>createTrue();
       if(ordersNotNull)
           whereProp = DerivedProperty.createAnd(innerInterfaces, whereProp, orders.keys());
       return DerivedProperty.createIfElseUProp(innerInterfaces, whereProp,
               action.mapCalcWhereProperty(), elseAction != null ? elseAction.mapCalcWhereProperty() : null);
    }

    protected ImSet<I> getExtendInterfaces() {
        ImSet<I> result = super.getExtendInterfaces();
        if(addObject != null)
            result = result.removeIncl(addObject);
        return result;
    }

    private ImMap<I, ValueClass> getExtendClasses() {
        if(ifProp==null)
            return MapFact.<I, ValueClass>EMPTY();
        assert forIsFull();
        return ifProp.mapInterfaceClasses(ClassType.forPolicy).remove(mapInterfaces.valuesSet()); // вообще тут предполагается ASSERTFULL, но только для extend interfaces, а пока такой возможности нет
    }

    private boolean forIsFull() {
        return ifProp == null || ifProp.mapIsFull(getExtendInterfaces());
    }

    private boolean isHackAdd() { // хак который используется только для реализации агрегаций, когда генерится FOR ADDOBJ t, затем CHANGE CLASS t TO X, который компиляция сворачивать в FOR ADDOBJ t=X (непонятно какой конкретный класс по умолчанию подставлять)  
        return addObject != null && !(addClass instanceof ConcreteCustomClass);
    }
    
    @Override
    @IdentityLazy
    protected boolean forceCompile() {
        return isHackAdd() | !forIsFull(); // очень тормозит
    }

    @Override
    public ActionPropertyMapImplement<?, I> compileExtend() { // проталкивание FOR'ов

        if(recursive)
            return null;
        
        ImSet<I> context = mapInterfaces.valuesSet();
        assert innerInterfaces.size() > context.size();
        boolean allNoInline = (innerInterfaces.size() == context.size() + noInline.size() + (addObject != null ? 1 : 0));

        if(!allNoInline && noInline.size() > 0) {
            assert !noInline.intersect(context);
            assert orders.isEmpty();

            MList<ActionPropertyMapImplement<?, I>> mResult = ListFact.mList();
            ImSet<I> extNoInline = context.addExcl(noInline);

            CalcPropertyMapImplement<?, I> noInlineIfProp = ifProp;
            ImSet<I> noInlineInterfaces = extNoInline;
            MSet<SessionDataProperty> mLocals = SetFact.mSet();
            if(CalcProperty.depends(ifProp.property, StoredDataProperty.set)) { // нужно создать сначала материалайзить условие for по аналогии с проталкиванием
                noInlineIfProp = DerivedProperty.createForDataProp(getExtendClasses(), ifProp.property.getValueClass(ClassType.forPolicy), mLocals);// делаем SET в session свойство, и подменяем условие на это свойство
                mResult.add(DerivedProperty.createSetAction(addObject != null ? innerInterfaces.removeIncl(addObject) : innerInterfaces, context, null, noInlineIfProp, ifProp));
                noInlineInterfaces = noInline;
            }

            // затем сделать GROUP ANY TRUE IF с группировкой по noInline интерфейсам, затем
            CalcPropertyMapImplement<?, I> groupNoInline = DerivedProperty.createAnyGProp(noInlineIfProp, noInlineInterfaces);
            // по нему уже сгруппировать FOR noInline интерфейсам с опцией Inline.NO, а внутри FOR по материализованному условию где noInline уже будут внешними интерфейсами
            ActionPropertyMapImplement<?, I> cleanAction = createForAction(innerInterfaces, extNoInline, noInlineIfProp, MapFact.<CalcPropertyInterfaceImplement<I>, Boolean>EMPTYORDER(), false,
                    action, null, addObject, addClass, recursive, SetFact.<I>EMPTY(), forceInline);
            mResult.add(createForAction(extNoInline, context, groupNoInline, MapFact.<CalcPropertyInterfaceImplement<I>, Boolean>EMPTYORDER(), false,
                    cleanAction, elseAction, false, noInline, false));
            return DerivedProperty.createListAction(context, mResult.immutableList(), mLocals.immutable());
        }

        boolean hackAdd = isHackAdd();
        if(allNoInline && !hackAdd)
            return null;

        if (addObject != null) { // "компиляция" ADDOBJ
            // сначала проверим если первый в списке CHANGE CLASS, тогда заберем его в FOR
            ImList<ActionPropertyMapImplement<?, I>> list = action.getList();

            if (list.size() > 0) {
                ActionPropertyMapImplement<?, I> first = list.get(0);
                if (first.mapping.size() == 1 && first.mapping.singleValue().equals(addObject) && first.property instanceof ChangeClassActionProperty) {
                    ChangeClassActionProperty changeClassProperty = (ChangeClassActionProperty) first.property;
                    if (changeClassProperty.valueClass instanceof CustomClass && changeClassProperty.where == null) // удаление не интересует
                        return DerivedProperty.createForAction(innerInterfaces, context, ifProp, orders, ordersNotNull,
                                DerivedProperty.createListAction(innerInterfaces, list.subList(1, list.size())), elseAction, addObject,
                                (CustomClass) changeClassProperty.valueClass, recursive, noInline, forceInline);
                }
            }
        }

        // проталкиваем for'ы
        if (action.hasFlow(ChangeFlowType.BREAK, ChangeFlowType.APPLY, ChangeFlowType.CANCEL, ChangeFlowType.VOLATILE))
            return null;

        if(addObject != null) {
            assert !hackAdd; // должен отработать сверху

            MSet<SessionDataProperty> mLocals = SetFact.mSet();
            CalcPropertyMapImplement<?, I> result = DerivedProperty.createForDataProp(getExtendClasses(), addClass, mLocals);
            return DerivedProperty.createListAction(context, ListFact.<ActionPropertyMapImplement<?, I>>toList(
                    DerivedProperty.createAddAction(addClass, innerInterfaces.removeIncl(addObject), context, ifProp, result, orders, ordersNotNull),
                    DerivedProperty.createForAction(innerInterfaces, context, DerivedProperty.<I>createCompare(
                            addObject, result, Compare.EQUALS), MapFact.<CalcPropertyInterfaceImplement<I>, Boolean>singletonOrder(addObject, false), false, action, elseAction, null, null, false, allNoInline ? noInline.addExcl(addObject) : noInline, forceInline)), mLocals.immutable());
        }

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
        MSet<SessionDataProperty> mLocals = SetFact.mSet();

        CalcPropertyMapImplement<?, I> pushProp = ifProp;
        if ((canBePushed.size() + (rest.size() > 0 ? 1 : 0) > 1)) {// если кол-во(вытаскиваемые+оставшиеся) > 1
            if (CalcProperty.dependsImplement(orders.keys(), pushChangedProps)) // если orders'ы меняются пока не проталкиваем
                return null;

            if (CalcProperty.depends(ifProp.property, pushChangedProps) || // если есть stored свойства (а не чисто session) или меняет условия
                    CalcProperty.depends(ifProp.property, StoredDataProperty.set)) {
                pushProp = DerivedProperty.createForDataProp(getExtendClasses(), ifProp.property.getValueClass(ClassType.forPolicy), mLocals); // делаем SET в session свойство, и подменяем условие на это свойство
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

        return DerivedProperty.createListAction(context, mResult.immutableList(), mLocals.immutable());
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
                return createForAction(context, where, orders.mergeOrder(mapImplements(ForActionProperty.this.orders, mapInnerInterfaces)), ordersNotNull, action.map(mapInnerInterfaces), null, addObject != null ? mapInnerInterfaces.get(addObject): null, addClass, false, noInline.mapRev(mapInnerInterfaces), forceInline);
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

    public interface PushFor<PI extends PropertyInterface, I extends PropertyInterface> {
        ActionPropertyMapImplement<?, I> push(ImSet<I> context, CalcPropertyMapImplement<?, I> where, ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, ImRevMap<PI, I> mapInnerInterfaces);
    }

    public static <T extends PropertyInterface, I extends PropertyInterface, W extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?, T> pushFor(
            ImSet<I> innerInterfaces, CalcPropertyInterfaceImplement<I> forProp, ImRevMap<PropertyInterface, I> mapInterfaces, ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, CalcPropertyMapImplement<PW, T> push,
            ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull, PushFor<I, PropertyInterface> pushFor) {
        assert !ordersNotNull; // в противном случае придется еще с orders'ов собирать интерфейсы
        assert mapInterfaces.keys().equals(mapping.keys());

        // сначала and'им where и push, получаем интерфейсы I + push (T)
        Result<ImRevMap<T, PropertyInterface>> mapPushInterfaces = new Result<>(); Result<ImRevMap<I, PropertyInterface>> mapInnerInterfaces = new Result<>();
        createCommon(mapping.valuesSet().merge(push.mapping.valuesSet()), innerInterfaces, mapping.crossJoin(mapInterfaces), mapPushInterfaces, mapInnerInterfaces);

        CalcPropertyMapImplement<?, PropertyInterface> mapPush = push.map(mapPushInterfaces.result);
        if(forProp!=null)
            mapPush = createAnd(mapPush, forProp.map(mapInnerInterfaces.result));

        return pushFor.push(mapPushInterfaces.result.filterRev(context).valuesSet(), mapPush,
                DerivedProperty.mapImplements(orders, mapPushInterfaces.result), ordersNotNull, mapInnerInterfaces.result).map(mapPushInterfaces.result.reverse());
    }

    @Override
    public boolean ignoreReadOnlyPolicy() {
        return action != null && action.property.ignoreReadOnlyPolicy() && (elseAction == null || elseAction.property.ignoreReadOnlyPolicy());
    }
}
