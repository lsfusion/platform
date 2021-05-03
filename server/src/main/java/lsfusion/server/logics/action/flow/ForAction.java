package lsfusion.server.logics.action.flow;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingFunction;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.base.controller.stack.ParamMessage;
import lsfusion.server.base.controller.stack.ProgressStackItem;
import lsfusion.server.base.controller.stack.ThisMessage;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.change.AddObjectAction;
import lsfusion.server.logics.action.change.ChangeClassAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.data.PropertyOrderSet;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClasses;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.AbstractCustomClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.log.LogTime;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;

import static lsfusion.server.logics.property.PropertyFact.*;

public class ForAction<I extends PropertyInterface> extends ExtendContextAction<I> {
   
    private final PropertyMapImplement<?, I> ifProp; // calculate
    private final ImOrderMap<PropertyInterfaceImplement<I>, Boolean> orders; // calculate
    private final boolean ordersNotNull;
    private final ActionMapImplement<?, I> action; // action
    private final ActionMapImplement<?, I> elseAction; // action
    private final boolean recursive;

    private final I addObject;
    private final CustomClass addClass;
    private final boolean autoSet;

    private final ImSet<I> noInline; // из extend interfaces
    private final boolean forceInline;
   
    public ForAction(LocalizedString caption, ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces, PropertyMapImplement<?, I> ifProp, ImOrderMap<PropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, ActionMapImplement<?, I> action, ActionMapImplement<?, I> elseAction, I addObject, CustomClass addClass, boolean autoSet, boolean recursive, ImSet<I> noInline, boolean forceInline) {
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
        this.autoSet = autoSet;

        this.noInline = noInline;
        this.forceInline = forceInline;

        assert (addObject==null || !noInline.contains(addObject)) && !noInline.intersect(mapInterfaces.getSet()) && innerInterfaces.containsAll(noInline);

        finalizeInit();
        assert innerInterfaces.containsAll(action.mapping.valuesSet().merge(ifProp != null ? ifProp.mapping.valuesSet() : SetFact.EMPTY()));
    }

    public ImSet<Action> getDependActions() {
       ImSet<Action> result = SetFact.singleton(action.action);
       if(elseAction != null)
           result = result.merge(elseAction.action);
       return result;
    }

    @Override
    public ImMap<Property, Boolean> aspectUsedExtProps() {
       MSet<Property> mUsed = SetFact.mSet();
       if(ifProp!=null)
           ifProp.mapFillDepends(mUsed);
       for(PropertyInterfaceImplement<I> order : orders.keyIt())
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

        public void updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
            final ImOrderSet<ImMap<I, DataObject>> prevRows = rows;
            session.addRollbackInfo(() -> rows = prevRows);
            rows = rows.subOrder(i, rows.size()).<SQLException, SQLHandledException>mapItIdentityOrderValuesEx(value -> session.updateCurrentClasses(value));
            i=0;
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
            String progressCaption = toString();
            ProgressStackItem stackItem = ExecutionStackAspect.pushProgressStackItem(progressCaption, 0, rowUpdate.rows.size());
            try {
                int size = rowUpdate.rows.size();
                boolean overrideMoreSessionUsages = !context.hasMoreSessionUsages && (recursive || size > 1) && hasFlow(ChangeFlowType.NEEDMORESESSIONUSAGES) && hasFlow(ChangeFlowType.HASSESSIONUSAGES);
                for (int i = 0; i < size; i++) {
                    ImMap<I, DataObject> row = rowUpdate.rows.get(i);
                    ImMap<I, ObjectValue> newValues = MapFact.addExcl(innerValues, row);
                    if(addObject!=null)
                        newValues = MapFact.addExcl(newValues, addObject, context.addObject((ConcreteCustomClass) addClass, autoSet));

                    ExecutionStackAspect.popProgressStackItem(stackItem);
                    stackItem = ExecutionStackAspect.pushProgressStackItem(progressCaption, i + 1, rowUpdate.rows.size());

                    FlowResult actionResult = executeFor(overrideMoreSessionUsages && (recursive || i < size - 1) // is not last 
                                                        ? context.override(true): context, newValues);
                    if (actionResult != FlowResult.FINISH) {
                        if (actionResult != FlowResult.BREAK) {
                            result = actionResult;
                        }
                        break RECURSIVE;
                    }
                }
            } finally {
                context.popUpdate();
                ExecutionStackAspect.popProgressStackItem(stackItem);
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

    private static <P extends PropertyInterface, M extends  PropertyInterface> FlowResult execute(ExecutionContext<PropertyInterface> context, ActionMapImplement<P, M> implement, ImMap<M, ? extends ObjectValue> keys, ImRevMap<PropertyInterface, M> mapInterfaces) throws SQLException, SQLHandledException {
        return implement.action.execute(
                context.override(
                        implement.mapping.join(keys),
                        BaseUtils.<ImMap<P, PropertyInterfaceImplement<PropertyInterface>>>immutableCast(
                                MapFact.innerCrossValues(implement.mapping, mapInterfaces)
                        )
                )
        );
    }

    private ImOrderSet<ImMap<I, DataObject>> readRows(final ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends Expr> innerExprs) throws SQLException, SQLHandledException {
        if(ifProp == null)
            return SetFact.singletonOrder(MapFact.EMPTY());
            
        Where where = ifProp.mapExpr(innerExprs, context.getModifier()).getWhere();

        final ImMap<I, ? extends Expr> fInnerExprs = PropertyChange.simplifyExprs(innerExprs, where);
        ImOrderMap<Expr, Boolean> orderExprs = orders.mapMergeOrderKeysEx((ThrowingFunction<PropertyInterfaceImplement<I>, Expr, SQLException, SQLHandledException>) value -> value.mapExpr(fInnerExprs, context.getModifier()));

        return new PropertyOrderSet<>(innerKeys, where, orderExprs, ordersNotNull).executeClasses(context.getEnv());
    }

    protected PropertyMapImplement<?, I> calcGroupWhereProperty() {
       PropertyMapImplement<?, I> whereProp = ifProp != null ? ifProp : PropertyFact.createTrue();
       if(ordersNotNull)
           whereProp = PropertyFact.createAnd(innerInterfaces, whereProp, orders.keys());
       return PropertyFact.createIfElseUProp(innerInterfaces, whereProp,
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
            return MapFact.EMPTY();
        assert forIsFull();
        return ifProp.mapInterfaceClasses(ClassType.forPolicy).remove(mapInterfaces.valuesSet()); // вообще тут предполагается ASSERTFULL, но только для extend interfaces, а пока такой возможности нет
    }

    private boolean forIsFull() {
        return ifProp == null || ifProp.mapIsFull(getExtendInterfaces());
    }

    private boolean needDialog() {
        assert addObject != null;
        return addClass instanceof AbstractCustomClass; 
    }
    
    private boolean isHackAdd() { // хак который используется только для реализации агрегаций, когда генерится FOR NEW t, затем CHANGE CLASS t TO X, который компиляция сворачивать в FOR NEW t=X (непонятно какой конкретный класс по умолчанию подставлять)
        return addObject != null && needDialog();
    }
    
    @Override
    @IdentityLazy
    protected boolean forceCompile() {
        return isHackAdd() || !forIsFull(); // очень тормозит
    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        ImMap<Property, Boolean> result = super.aspectChangeExtProps();
        if(addObject != null) // может быть, из-за break, noinline и т.п.
            result = result.merge(AddObjectAction.getChangeExtProps(addClass, needDialog()), addValue);
        return result;
    }

    @Override
    public ActionMapImplement<?, I> compileExtend() { // проталкивание FOR'ов

        if(recursive)
            return null;

        if(addObject != null && ifProp != null && autoSet)
            return null;

        ImSet<I> context = mapInterfaces.valuesSet();
        assert innerInterfaces.size() > context.size();
        boolean allNoInline = (innerInterfaces.size() == context.size() + noInline.size() + (addObject != null ? 1 : 0));
        
        if(noInline.size() > 0 && !orders.isEmpty()) // пока этот случай не поддерживается
            allNoInline = true;

        if(!allNoInline && noInline.size() > 0) {
            assert !noInline.intersect(context);
            assert orders.isEmpty(); // см. проверку сверху

            MList<ActionMapImplement<?, I>> mResult = ListFact.mList();
            ImSet<I> extNoInline = context.addExcl(noInline);

            PropertyMapImplement<?, I> noInlineIfProp = ifProp;
            ImSet<I> noInlineInterfaces = extNoInline;
            MSet<SessionDataProperty> mLocals = SetFact.mSet();
            if(Property.depends(ifProp.property, StoredDataProperty.set)) { // нужно создать сначала материалайзить условие for по аналогии с проталкиванием
                noInlineIfProp = PropertyFact.createForDataProp(getExtendClasses(), ifProp.property.getValueClass(ClassType.forPolicy), mLocals);// делаем SET в session свойство, и подменяем условие на это свойство
                mResult.add(PropertyFact.createSetAction(addObject != null ? innerInterfaces.removeIncl(addObject) : innerInterfaces, context, null, noInlineIfProp, ifProp));
                noInlineInterfaces = noInline;
            }

            // затем сделать GROUP ANY TRUE IF с группировкой по noInline интерфейсам, затем
            PropertyMapImplement<?, I> groupNoInline = PropertyFact.createAnyGProp(noInlineIfProp, noInlineInterfaces);
            // по нему уже сгруппировать FOR noInline интерфейсам с опцией Inline.NO, а внутри FOR по материализованному условию где noInline уже будут внешними интерфейсами
            ActionMapImplement<?, I> cleanAction = createForAction(innerInterfaces, extNoInline, noInlineIfProp, MapFact.EMPTYORDER(), false,
                    action, null, addObject, addClass, autoSet, recursive, SetFact.EMPTY(), forceInline);
            mResult.add(createForAction(extNoInline, context, groupNoInline, MapFact.EMPTYORDER(), false,
                    cleanAction, elseAction, false, noInline, false));
            return PropertyFact.createListAction(context, mResult.immutableList(), mLocals.immutable());
        }

        boolean hackAdd = isHackAdd();
        if(allNoInline && !hackAdd)
            return null;

        if (addObject != null) { // "компиляция" NEW
            // сначала проверим если первый в списке CHANGE CLASS, тогда заберем его в FOR
            ImList<ActionMapImplement<?, I>> list = action.getList();

            if (list.size() > 0) {
                ActionMapImplement<?, I> first = list.get(0);
                if (first.mapping.size() == 1 && first.mapping.singleValue().equals(addObject) && first.action instanceof ChangeClassAction) {
                    ChangeClassAction changeClassProperty = (ChangeClassAction) first.action;
                    if (changeClassProperty.valueClass instanceof CustomClass && changeClassProperty.where == null) // удаление не интересует
                        return PropertyFact.createForAction(innerInterfaces, context, ifProp, orders, ordersNotNull,
                                PropertyFact.createListAction(innerInterfaces, list.subList(1, list.size())), elseAction, addObject,
                                (CustomClass) changeClassProperty.valueClass, autoSet, recursive, noInline, forceInline);
                }
            }
        }

        if(addObject != null) {
            MSet<SessionDataProperty> mLocals = SetFact.mSet();
            PropertyMapImplement<?, I> result = PropertyFact.createForDataProp(getExtendClasses(), addClass, mLocals);
            return PropertyFact.createListAction(context, ListFact.<ActionMapImplement<?, I>>toList(
                    PropertyFact.createAddAction(addClass, innerInterfaces.removeIncl(addObject), context, ifProp, result, orders, ordersNotNull, autoSet),
                    PropertyFact.createForAction(innerInterfaces, context, PropertyFact.<I>createCompare(
                            addObject, result, Compare.EQUALS), MapFact.<PropertyInterfaceImplement<I>, Boolean>singletonOrder(addObject, false), false, action, elseAction, null, null, false, false, allNoInline ? noInline.addExcl(addObject) : noInline, forceInline)), mLocals.immutable());
        }

        // проталкиваем for'ы
        if (action.hasFlow(ChangeFlowType.BREAK, ChangeFlowType.RETURN, ChangeFlowType.APPLY, ChangeFlowType.CANCEL, ChangeFlowType.SYNC))
            return null;

        ImList<ActionMapImplement<?, I>> list = action.getList();

        ImSet<Property>[] listChangeProps = new ImSet[list.size()];
        ImSet<Property>[] listUsedProps = new ImSet[list.size()];
        for (int i = 0; i < list.size(); i++) {
            listChangeProps[i] = list.get(i).action.getChangeProps();
            listUsedProps[i] = list.get(i).action.getUsedProps();
        }

        // ищем сначала "вытаскиваемые" (changeProps не зависят от usedProps и т.д)
        final MSet<Property> mPushChangedProps = SetFact.mSet();
        MList<ActionMapImplement<?, I>> mCanBePushed = ListFact.mFilter(list);
        MList<ActionMapImplement<?, I>> mRest = ListFact.mFilter(list);
        for (int i = 0; i < list.size(); i++) {
            ActionMapImplement<?, I> itemAction = list.get(i);

            if (itemAction.hasPushFor(context, ordersNotNull)) {
                MSet<Property> mSiblingChangeProps = SetFact.mSet();
                MSet<Property> mSiblingUsedProps = SetFact.mSet();
                for (int j = 0; j < list.size(); j++) // читаем sibling'и
                    if (j != i) {
                        mSiblingChangeProps.addAll(listChangeProps[j]);
                        mSiblingUsedProps.addAll(listUsedProps[j]);
                    }
                ImSet<Property> siblingChangeProps = mSiblingChangeProps.immutable();
                ImSet<Property> siblingUsedProps = mSiblingUsedProps.immutable();

                ImSet<Property> changeProps = listChangeProps[i];
                ImSet<Property> usedProps = listUsedProps[i];

                Property where = itemAction.getPushWhere(context, ordersNotNull);
                if (forceInline || (!Property.depends(siblingUsedProps, changeProps) && // не меняют сиблингов
                        !Property.depends(usedProps, siblingChangeProps) && // сиблинги не меняют
                        !Property.depends(where!=null?Collections.singleton(where):usedProps, changeProps) && // не рекурсивно зависимо
                        siblingChangeProps.disjoint(changeProps))) { // несколько раз не меняется
                    mCanBePushed.add(itemAction);
                    mPushChangedProps.addAll(changeProps);
                } else
                    mRest.add(itemAction);
            } else
                mRest.add(itemAction);
        }
        ImSet<Property> pushChangedProps = mPushChangedProps.immutable();
        ImList<ActionMapImplement<?, I>> canBePushed = ListFact.imFilter(mCanBePushed, list);
        ImList<ActionMapImplement<?, I>> rest = ListFact.imFilter(mRest, list);

        if (canBePushed.size() == 0)
            return null;

        MList<ActionMapImplement<?, I>> mResult = ListFact.mList();
        MSet<SessionDataProperty> mLocals = SetFact.mSet();

        PropertyMapImplement<?, I> pushProp = ifProp;
        if ((canBePushed.size() + (rest.size() > 0 ? 1 : 0) > 1)) {// если кол-во(вытаскиваемые+оставшиеся) > 1
            if (Property.dependsImplement(orders.keys(), pushChangedProps)) // если orders'ы меняются пока не проталкиваем
                return null;

            if (Property.depends(ifProp.property, pushChangedProps) || // если есть stored свойства (а не чисто session) или меняет условия
                    Property.depends(ifProp.property, StoredDataProperty.set)) {
                pushProp = PropertyFact.createForDataProp(getExtendClasses(), ifProp.property.getValueClass(ClassType.forPolicy), mLocals); // делаем SET в session свойство, и подменяем условие на это свойство
                mResult.add(PropertyFact.createSetAction(innerInterfaces, context, null, pushProp, ifProp));
            }
        }

        // "вытаскиваемым" проталкиваем where + order и добавляем в начало
        for (ActionMapImplement<?, I> property : canBePushed)
            mResult.add(property.pushFor(context, pushProp, orders, ordersNotNull));

        // добавляем оставшиеся, с for'ом компилируя внутренние элементы
        if (rest.size() > 0 || elseAction != null)
            mResult.add(PropertyFact.createForAction(innerInterfaces, context, pushProp, orders,
                    ordersNotNull, PropertyFact.createListAction(innerInterfaces, rest), elseAction, false, innerInterfaces.remove(context), false));

        return PropertyFact.createListAction(context, mResult.immutableList(), mLocals.immutable());
    }

    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return elseAction==null && !hasFlow(ChangeFlowType.BREAK) && ForAction.this.ordersNotNull == ordersNotNull && elseAction == null && !recursive; // потом отработаем эти случаи
    }
    
    // nullable
    public static <I extends PropertyInterface> Property getPushWhere(PropertyInterfaceImplement<I> where) {
        return where instanceof PropertyMapImplement ? ((PropertyMapImplement) where).property : PropertyFact.createTrue().property; // тут не null должен возвращаться 
    }
    
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> Property getPushWhere(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        return getPushWhere(ifProp);
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionMapImplement<?, T> pushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, PropertyMapImplement<PW, T> push, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        return pushFor(innerInterfaces, ifProp, mapInterfaces, mapping, context, push, orders, ordersNotNull, (context1, where, orders1, ordersNotNull1, mapInnerInterfaces) -> createForAction(context1, where, orders1.mergeOrder(mapImplements(ForAction.this.orders, mapInnerInterfaces)), ordersNotNull1, action.map(mapInnerInterfaces), null, addObject != null ? mapInnerInterfaces.get(addObject): null, addClass, autoSet, false, noInline.mapRev(mapInnerInterfaces), forceInline));
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
        if (type == ChangeFlowType.BREAK)
            return false;
        if (addObject != null && type.isChange())
            return true;            
        return super.hasFlow(type);
    }

    public interface PushFor<PI extends PropertyInterface, I extends PropertyInterface> {
        ActionMapImplement<?, I> push(ImSet<I> context, PropertyMapImplement<?, I> where, ImOrderMap<PropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, ImRevMap<PI, I> mapInnerInterfaces);
    }

    public static <T extends PropertyInterface, I extends PropertyInterface, W extends PropertyInterface, PW extends PropertyInterface> ActionMapImplement<?, T> pushFor(
            ImSet<I> innerInterfaces, PropertyInterfaceImplement<I> forProp, ImRevMap<PropertyInterface, I> mapInterfaces, ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, PropertyMapImplement<PW, T> push,
            ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull, PushFor<I, PropertyInterface> pushFor) {
        assert !ordersNotNull; // в противном случае придется еще с orders'ов собирать интерфейсы
        assert mapInterfaces.keys().equals(mapping.keys());

        // сначала and'им where и push, получаем интерфейсы I + push (T)
        Result<ImRevMap<I, PropertyInterface>> mapInnerInterfaces = new Result<>();
        ImRevMap<T, PropertyInterface> mapPushInterfaces = createCommon(mapping.valuesSet().merge(push.mapping.valuesSet()), innerInterfaces, mapping.crossJoin(mapInterfaces), mapInnerInterfaces);

        PropertyMapImplement<?, PropertyInterface> mapPush = push.map(mapPushInterfaces);
        if(forProp!=null)
            mapPush = createAnd(mapPush, forProp.map(mapInnerInterfaces.result));

        return pushFor.push(mapPushInterfaces.filterRev(context).valuesSet(), mapPush,
                PropertyFact.mapImplements(orders, mapPushInterfaces), ordersNotNull, mapInnerInterfaces.result).map(mapPushInterfaces.reverse());
    }

    @Override
    public ActionMapImplement<?, I> replaceExtend(ActionReplacer replacer) {
        ActionMapImplement<?, I> replacedAction = action.mapReplaceExtend(replacer);
        ActionMapImplement<?, I> replacedElseAction = elseAction != null ? elseAction.mapReplaceExtend(replacer) : null;
        if(replacedAction == null && replacedElseAction == null)
            return null;
        
        if(replacedAction == null)
            replacedAction = action;
        if(replacedElseAction == null)
            replacedElseAction = elseAction;
        return PropertyFact.createForAction(innerInterfaces, mapInterfaces.valuesSet(), ifProp, orders, ordersNotNull, replacedAction, replacedElseAction, addObject, addClass, autoSet, recursive, noInline, forceInline);
    }

    @Override
    public AsyncMapEventExec<PropertyInterface> calculateAsyncEventExec(boolean optimistic, boolean recursive) {
        AsyncMapEventExec<I> asyncExec = getBranchAsyncEventExec(ListFact.toList(action, elseAction), optimistic, recursive);
        if(asyncExec != null)
            return asyncExec.mapInner(mapInterfaces.reverse());
        return null;
    }

}
