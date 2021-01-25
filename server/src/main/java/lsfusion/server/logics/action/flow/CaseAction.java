package lsfusion.server.logics.action.flow;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NFListImpl;
import lsfusion.server.base.version.interfaces.NFList;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.OrObjectClassSet;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.form.struct.property.async.AsyncOpenForm;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.cases.*;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static lsfusion.server.logics.property.PropertyFact.createForAction;

public class CaseAction extends ListCaseAction {

    public static <I extends PropertyInterface> Action createIf(LocalizedString caption, boolean not, ImOrderSet<I> innerInterfaces, PropertyInterfaceImplement<I> ifProp, ActionMapImplement<?, I> trueAction, ActionMapImplement<?, I> falseAction) {
        assert trueAction != null;
        if(not) // просто not'им if
            ifProp = PropertyFact.createNot(ifProp);

        MList<ActionCase<I>> mCases = ListFact.mListMax(2);
        mCases.add(new ActionCase<>(ifProp, trueAction));
        if(falseAction != null)
            mCases.add(new ActionCase<>(PropertyFact.createTrue(), falseAction));
        return new CaseAction(caption, false, innerInterfaces, mCases.immutableList());
    }

    public void addCase(PropertyMapImplement<?, PropertyInterface> where, ActionMapImplement<?, PropertyInterface> action, Version version) {
        assert type == AbstractType.CASE;

        ExplicitActionCase<PropertyInterface> aCase = new ExplicitActionCase<>(where, action);
        addAbstractCase(aCase, version);

        addWhereCase(aCase.where, aCase.implement, version);
    }

    private Object cases;

    public void addImplicitCase(ActionMapImplement<?, PropertyInterface> aCase, List<ResolveClassSet> signature, boolean sameNamespace, Version version) {
        addAbstractCase(new ImplicitActionCase(aCase, signature, sameNamespace), version);        
    }

    private void addAbstractCase(AbstractActionCase<PropertyInterface> aCase, Version version) {
        NFListImpl.add(isLast, (NFList<AbstractActionCase<PropertyInterface>>) cases, aCase, version);
    }

    public void addOperand(ActionMapImplement<?,PropertyInterface> action, List<ResolveClassSet> signature, Version version) {
        assert isAbstract();

        PropertyMapImplement<?, PropertyInterface> where =  action.mapWhereProperty();
        ExplicitActionCase<PropertyInterface> addCase;
        if(type == AbstractType.MULTI)
            addCase = new ExplicitActionCase<>(where.mapClassProperty(), action, signature);
        else
            addCase = new ExplicitActionCase<>(where, action);
        addAbstractCase(addCase, version);

        addWhereOperand(addCase.implement, signature, version);
    }
    
    private ImList<ActionCase<PropertyInterface>> getCases() {
        return ((ImList<ActionCase<PropertyInterface>>)cases);
    }

    @Override
    protected ActionMapImplement<?, PropertyInterface> aspectReplace(ActionReplacer replacer) {
        ImList<ActionCase<PropertyInterface>> cases = getCases();
        ImList<ActionCase<PropertyInterface>> replacedCases = cases.mapListValues((ActionCase<PropertyInterface> aCase) -> {
            ActionMapImplement<?, PropertyInterface> implementReplace = aCase.implement.mapReplaceExtend(replacer);
            if (implementReplace == null) 
                return null;
            return new ActionCase<>(aCase.where, implementReplace);
        });
        
        if(replacedCases.filterList(Objects::nonNull).isEmpty())
            return null;
        
        return PropertyFact.createCaseAction(interfaces, isExclusive, replacedCases.mapListValues((i, aCase) -> {
            if(aCase == null)
                return cases.get(i);
            return aCase;
        }));        
    }

    public <I extends PropertyInterface> CaseAction(LocalizedString caption, boolean isExclusive, ImList<ActionMapImplement> impls, ImOrderSet<I> innerInterfaces) {
        this(caption, isExclusive, innerInterfaces, impls.mapListValues((ActionMapImplement value) -> new ActionCase<I>(value.mapWhereProperty().mapClassProperty(), value)));
    }

    // explicit конструктор
    public <I extends PropertyInterface> CaseAction(LocalizedString caption, boolean isExclusive, ImOrderSet<I> innerInterfaces, ImList<ActionCase<I>> cases)  {
        super(caption, isExclusive, innerInterfaces);

        final ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.cases = cases.mapListValues((Function<ActionCase<I>, ActionCase<PropertyInterface>>) value -> value.map(mapInterfaces));

        finalizeInit();
    }

    public <I extends PropertyInterface> CaseAction(LocalizedString caption, boolean isExclusive, boolean isChecked, boolean isLast, AbstractType type, ImOrderSet<I> innerInterfaces, ImMap<I, ValueClass> mapClasses)  {
        super(caption, isExclusive, isChecked, isLast, type, innerInterfaces, mapClasses);

        cases = NFFact.list();
    }

    protected PropertyMapImplement<?, PropertyInterface> calcCaseWhereProperty() {
        ImList<CalcCase<PropertyInterface>> listWheres = getCases().mapListValues((Function<ActionCase<PropertyInterface>, CalcCase<PropertyInterface>>) value -> new CalcCase<>(value.where, value.implement.mapCalcWhereProperty()));
        return PropertyFact.createUnion(interfaces, isExclusive, listWheres);
    }

    protected ImList<ActionMapImplement<?, PropertyInterface>> getListActions() {
        return getCases().mapListValues((Function<ActionCase<PropertyInterface>, ActionMapImplement<?, PropertyInterface>>) value -> value.implement);
    }

    @Override
    public ImMap<Property, Boolean> aspectUsedExtProps() {
        ImList<ActionCase<PropertyInterface>> cases = getCases();
        MSet<Property> mWhereProps = SetFact.mSetMax(cases.size());
        for(ActionCase<PropertyInterface> aCase : cases)
            if(aCase.where instanceof PropertyMapImplement)
                mWhereProps.add(((PropertyMapImplement) aCase.where).property);
        return mWhereProps.immutable().toMap(false).merge(super.aspectUsedExtProps(), addValue);
    }

    @IdentityLazy
    public ImList<ActionCase<PropertyInterface>> getOptimizedCases(final ImMap<PropertyInterface, ? extends AndClassSet> currentClasses, final ImSet<PropertyInterface> nulls) {
        return getCases().filterList(element -> {
            PropertyInterfaceImplement<PropertyInterface> where = element.where;
            if(where instanceof PropertyMapImplement) {
                PropertyMapImplement<?, PropertyInterface> mapWhere = (PropertyMapImplement<?, PropertyInterface>) where;
                if (!currentClasses.isEmpty() && mapWhere.mapIsFull(currentClasses.keys())) { // тут надо найти по-хорошему найти full подмножество, но пока и такой оптимизации достаточно
                    // isFull => isNotNull
                    if (mapWhere.mapClassWhere(ClassType.casePolicy).and(new ClassWhere<>(currentClasses)).isFalse()) // и классы не пересекаются
                        return false;
                }
                if(!nulls.isEmpty() && mapWhere.mapIsNotNull(nulls)) // тут надо по-хорошему по одному интерфейсу проверить, но пока и такой оптимизации достаточно   
                    return false;
            }
            return true;
        });
    }
    
    private boolean checkOptimizedCases(ExecutionContext<PropertyInterface> context, ImList<ActionCase<PropertyInterface>> optimizedCases) throws SQLException, SQLHandledException {
        ImSet<ActionCase<PropertyInterface>> setCases = optimizedCases.toOrderSet().getSet();
        for(ActionCase<PropertyInterface> aCase : getCases())
            if(!setCases.contains(aCase)) {
                if(aCase.where.read(context, context.getKeys()) != null) {
                    ServerLoggers.assertLog(false, "OPTIMIZED CASES ASSERTION : PROPERTY - " + this + ", CASE - " + aCase + ", PARAMS - " + context.getKeys() + ", CLASSES " + context.getSession().getCurrentClasses(DataObject.filterDataObjects(context.getKeys())));
                    return false;
                }
            }
        return true;
    }
    
    // проверяет на классы и notNull
    private ImList<ActionCase<PropertyInterface>> getOptimizedCases(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ImList<ActionCase<PropertyInterface>> cases = getCases();
        if(cases.size() < Settings.get().getClassOptimizationActionCasesCount())
            return cases;

        // берем current classes
        ImMap<PropertyInterface, ? extends ObjectValue> keys = context.getKeys();
        Result<ImSet<PropertyInterface>> rNulls = new Result<>();
        ImMap<PropertyInterface, DataObject> dataObjects = DataObject.splitDataObjects(keys, rNulls);
        ImList<ActionCase<PropertyInterface>> result = getOptimizedCases(context.getSession().getCurrentClasses(dataObjects), rNulls.result);
        assert checkOptimizedCases(context, result);
        return result;
    }
    
    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        FlowResult result = FlowResult.FINISH;
        for(ActionCase<PropertyInterface> aCase : getOptimizedCases(context)) {
            if(aCase.where.read(context, context.getKeys()) != null) {
                result = aCase.implement.execute(context);
                break;
            }
        }
        return result;
    }

    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return getCases().size()==1; // нужно разбивать на if true и if false, потом реализуем
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> Property getPushWhere(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        return ForAction.getPushWhere(getCases().single().where);
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionMapImplement<?, T> pushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, PropertyMapImplement<PW, T> push, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        final ActionCase<PropertyInterface> singleCase = getCases().single();
        return ForAction.pushFor(interfaces, singleCase.where, interfaces.toRevMap(), mapping, context, push, orders, ordersNotNull, (context1, where, orders1, ordersNotNull1, mapInnerInterfaces) -> createForAction(context1, where, orders1, ordersNotNull1, singleCase.implement.map(mapInnerInterfaces), null, false, SetFact.EMPTY(), false));
    }

    @Override
    protected void finalizeAbstractInit() {
        super.finalizeAbstractInit();
        
        FinalizeResult<ActionCase<PropertyInterface>> finalize = AbstractCase.finalizeActionCases(
                interfaces, (NFList<AbstractActionCase<PropertyInterface>>) cases, type == AbstractType.MULTI, checkExclusiveImplementations);
        cases = finalize.cases;
        isExclusive = finalize.isExclusive;
        abstractGraph = finalize.graph;
    }
    
    public Graph<ActionCase<PropertyInterface>> getAbstractGraph() {
        assert BusinessLogics.disableImplicitCases || (isAbstract() && type == AbstractType.MULTI);

        return BusinessLogics.disableImplicitCases ? null : abstractGraph;
    }

    public Graph<ActionCase<PropertyInterface>> abstractGraph; 

    @Override
    public Type getFlowSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        Type type = null;
        ImList<ActionMapImplement<?, PropertyInterface>> actions = getListActions();
        for (ActionMapImplement<?, PropertyInterface> action : actions) {
            Type actionRequestType = action.action.getSimpleRequestInputType(optimistic, inRequest);
            if (!optimistic && actionRequestType == null) {
                return null;
            }

            if (type == null) {
                type = actionRequestType;
            } else {
                if(actionRequestType != null) {
                    type = type.getCompatible(actionRequestType);
                    if (type == null) {
                        return null;
                    }
                }
            }
        }
        return type;
    }

    @Override
    public CustomClass getSimpleAdd() {

        if(!isExclusive && Settings.get().isDisableSimpleAddRemoveInNonExclCase())
            return null;

        OrObjectClassSet result = null;
        for (ActionMapImplement<?, PropertyInterface> action : getListActions()) {
            CustomClass simpleAdd = action.action.getSimpleAdd();
            if(simpleAdd==null) // значит есть case который не добавляет
                return null;

            OrObjectClassSet set = simpleAdd.getUpSet().getOr();
            if(result == null)
                result = set;
            else
                result = result.or(set);
        }
        return result != null ? result.getCommonClass() : null;
    }

    @Override
    public PropertyInterface getSimpleDelete() {

        if(isExclusive || !Settings.get().isDisableSimpleAddRemoveInNonExclCase()) {
            PropertyInterface result = null;
            for (ActionMapImplement<?, PropertyInterface> action : getListActions()) {
                PropertyInterface simpleDelete = action.mapSimpleDelete();
                if (simpleDelete != null && (result == null || BaseUtils.hashEquals(result, simpleDelete)))
                    result = simpleDelete;
                else { // значит есть case который не удаляет или удаляет что-то другое
                    result = null;
                    break;
                }
            }
            if (result != null)
                return result;
        }
        
        return super.getSimpleDelete();
    }

    @Override
    public AsyncOpenForm getOpenForm() {
        AsyncOpenForm result = null;
        for (ActionMapImplement<?, PropertyInterface> action : getListActions()) {
            AsyncOpenForm openForm = action.action.getOpenForm();
            if (openForm != null) {
                if (result == null) {
                    result = openForm;
                } else {
                    return null;
                }
            }
        }
        return result;
    }

    /*
    public class IfActionProperty extends KeepContextActionProperty {

    private final PropertyMapImplement<?, PropertyInterface> ifProp;
    private final ActionMapImplement<?, PropertyInterface> trueAction;
    private final ActionMapImplement<?, PropertyInterface> falseAction;

    public <I extends PropertyInterface> IfActionProperty(String sID, LocalizedString caption, boolean not, ImOrderSet<I> innerInterfaces, PropertyMapImplement<?, I> ifProp, ActionMapImplement<?, I> trueAction, ActionMapImplement<?, I> falseAction) {
        super(sID, caption, innerInterfaces.size());

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.ifProp = ifProp.map(mapInterfaces);
        ActionMapImplement<?, PropertyInterface> mapTrue = trueAction.map(mapInterfaces);
        ActionMapImplement<?, PropertyInterface> mapFalse = falseAction != null ? falseAction.map(mapInterfaces) : null;
        if (!not) {
            this.trueAction = mapTrue;
            this.falseAction = mapFalse;
        } else {
            this.trueAction = mapFalse;
            this.falseAction = mapTrue;
        }

        finalizeInit();
    }

    @IdentityInstanceLazy
    public PropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return PropertyFact.createIfElseUProp(interfaces, ifProp,
                trueAction != null ? trueAction.mapWhereProperty() : null,
                falseAction !=null ? falseAction.mapWhereProperty() : null);
    }

    public ImSet<ActionProperty> getDependActions() {
        ImSet<ActionProperty> result = SetFact.EMPTY();
        if (trueAction != null) {
            result = result.merge(trueAction.property);
        }
        if (falseAction != null) {
            result = result.merge(falseAction.property);
        }
        return result;
    }

    @Override
    public ImMap<Property, Boolean> aspectUsedExtProps() {
        MSet<Property> used = SetFact.mSet();
        ifProp.mapFillDepends(used);
        return used.immutable().toMap(false).merge(super.aspectUsedExtProps(), addValue);
    }

    @Override
    public Type getSimpleRequestInputType() {
        Type trueType = trueAction == null ? null : trueAction.property.getSimpleRequestInputType();
        Type falseType = falseAction == null ? null : falseAction.property.getSimpleRequestInputType();

        return trueType == null
               ? falseType
               : falseType == null
                 ? trueType
                 : trueType.getCompatible(falseType);
    }

    @Override
    public CustomClass getSimpleAdd() {
        return null; // пока ничего не делаем, так как на клиенте придется, "отменять" изменения
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        return null; // по аналогии с верхним
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        if (readIf(context)) {
            if (trueAction != null) {
                return trueAction.execute(context);
            }
        } else {
            if (falseAction != null) {
                return falseAction.execute(context);
            }
        }
        return FlowResult.FINISH;
    }

    private boolean readIf(ExecutionContext<PropertyInterface> context) throws SQLException {
        return ifProp.read(context, context.getKeys()) != null;
    }

    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return falseAction == null; // нужно разбивать на if true и if false, потом реализуем
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> Property getPushWhere(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        return ifProp.property;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionMapImplement<?, T> pushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, PropertyMapImplement<PW, T> push, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        return ForActionProperty.pushFor(interfaces, ifProp, interfaces.toRevMap(), mapping, context, push, orders, ordersNotNull, new ForActionProperty.PushFor<PropertyInterface, PropertyInterface>() {
            public ActionMapImplement<?, PropertyInterface> push(ImSet<PropertyInterface> context, PropertyMapImplement<?, PropertyInterface> where, ImOrderMap<PropertyInterfaceImplement<PropertyInterface>, Boolean> orders, boolean ordersNotNull, ImRevMap<PropertyInterface, PropertyInterface> mapInnerInterfaces) {
                return createForAction(context, where, orders, ordersNotNull, trueAction.map(mapInnerInterfaces), null, false, SetFact.<PropertyInterface>EMPTY(), false);
            }
        });
    }

}
     */
}
