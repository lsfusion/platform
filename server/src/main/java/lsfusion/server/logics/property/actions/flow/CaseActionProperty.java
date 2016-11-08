package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.Settings;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.OrObjectClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.impl.NFListImpl;
import lsfusion.server.logics.mutables.interfaces.NFList;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.cases.*;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.List;

import static lsfusion.server.logics.property.derived.DerivedProperty.createForAction;

public class CaseActionProperty extends ListCaseActionProperty {

    public static <I extends PropertyInterface> ActionProperty createIf(LocalizedString caption, boolean not, ImOrderSet<I> innerInterfaces, CalcPropertyMapImplement<?, I> ifProp, ActionPropertyMapImplement<?, I> trueAction, ActionPropertyMapImplement<?, I> falseAction) {
        assert trueAction != null;
        if(not) // просто not'им if
            ifProp = DerivedProperty.createNot(ifProp);

        MList<ActionCase<I>> mCases = ListFact.mListMax(2);
        mCases.add(new ActionCase<>(ifProp, trueAction));
        if(falseAction != null)
            mCases.add(new ActionCase<>(DerivedProperty.<I>createTrue(), falseAction));
        return new CaseActionProperty(caption, false, innerInterfaces, mCases.immutableList());
    }

    public void addCase(CalcPropertyMapImplement<?, PropertyInterface> where, ActionPropertyMapImplement<?, PropertyInterface> action, Version version) {
        assert type == AbstractType.CASE;

        ExplicitActionCase<PropertyInterface> aCase = new ExplicitActionCase<>(where, action);
        addAbstractCase(aCase, version);

        addWhereCase(aCase.where, aCase.implement, version);
    }

    private Object cases;

    public void addImplicitCase(ActionPropertyMapImplement<?, PropertyInterface> aCase, List<ResolveClassSet> signature, boolean sameNamespace, Version version) {
        addAbstractCase(new ImplicitActionCase(aCase, signature, sameNamespace), version);        
    }

    private void addAbstractCase(AbstractActionCase<PropertyInterface> aCase, Version version) {
        NFListImpl.add(isLast, (NFList<AbstractActionCase<PropertyInterface>>) cases, aCase, version);
    }

    public void addOperand(ActionPropertyMapImplement<?,PropertyInterface> action, List<ResolveClassSet> signature, Version version) {
        assert isAbstract();

        CalcPropertyMapImplement<?, PropertyInterface> where =  action.mapWhereProperty();
        ExplicitActionCase<PropertyInterface> addCase;
        if(type == AbstractType.CASE)
            addCase = new ExplicitActionCase<>((CalcPropertyMapImplement<?, PropertyInterface>) where.mapClassProperty(), action, signature);
        else
            addCase = new ExplicitActionCase<>(where, action);
        addAbstractCase(addCase, version);

        addWhereOperand(addCase.implement, signature, version);
    }
    
    private ImList<ActionCase<PropertyInterface>> getCases() {
        return ((ImList<ActionCase<PropertyInterface>>)cases);
    }

    public <I extends PropertyInterface> CaseActionProperty(LocalizedString caption, boolean isExclusive, ImList<ActionPropertyMapImplement> impls, ImOrderSet<I> innerInterfaces) {
        this(caption, isExclusive, innerInterfaces, impls.<ActionCase<I>>mapListValues(new GetValue<ActionCase<I>, ActionPropertyMapImplement>() {
            @Override
            public ActionCase<I> getMapValue(ActionPropertyMapImplement value) {
                return new ActionCase<>((CalcPropertyMapImplement) value.mapWhereProperty().mapClassProperty(), value);
            }
        }));
    }

    // explicit конструктор
    public <I extends PropertyInterface> CaseActionProperty(LocalizedString caption, boolean isExclusive, ImOrderSet<I> innerInterfaces, ImList<ActionCase<I>> cases)  {
        super(caption, isExclusive, innerInterfaces);

        final ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.cases = cases.mapListValues(new GetValue<ActionCase<PropertyInterface>, ActionCase<I>>() {
            public ActionCase<PropertyInterface> getMapValue(ActionCase<I> value) {
                return value.map(mapInterfaces);
            }
        });

        finalizeInit();
    }

    public <I extends PropertyInterface> CaseActionProperty(LocalizedString caption, boolean isExclusive, boolean isChecked, boolean isLast, AbstractType type, ImOrderSet<I> innerInterfaces, ImMap<I, ValueClass> mapClasses)  {
        super(caption, isExclusive, isChecked, isLast, type, innerInterfaces, mapClasses);

        cases = NFFact.list();
    }

    protected CalcPropertyMapImplement<?, PropertyInterface> calcCaseWhereProperty() {
        ImList<CalcCase<PropertyInterface>> listWheres = getCases().mapListValues(new GetValue<CalcCase<PropertyInterface>, ActionCase<PropertyInterface>>() {
            public CalcCase<PropertyInterface> getMapValue(ActionCase<PropertyInterface> value) {
                return new CalcCase<>(value.where, value.implement.mapCalcWhereProperty());
            }});
        return DerivedProperty.createUnion(interfaces, isExclusive, listWheres);
    }

    protected ImList<ActionPropertyMapImplement<?, PropertyInterface>> getListActions() {
        return getCases().mapListValues(new GetValue<ActionPropertyMapImplement<?, PropertyInterface>, ActionCase<PropertyInterface>>() {
            public ActionPropertyMapImplement<?, PropertyInterface> getMapValue(ActionCase<PropertyInterface> value) {
                return value.implement;
            }});
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        return getCases().mapListValues(new GetValue<CalcProperty, ActionCase<PropertyInterface>>() {
            public CalcProperty getMapValue(ActionCase<PropertyInterface> value) {
                return value.where.property;
            }}).toOrderSet().getSet().toMap(false).merge(super.aspectUsedExtProps(), addValue);
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        FlowResult result = FlowResult.FINISH;
        for(ActionCase<PropertyInterface> aCase : getCases()) {
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
    public <T extends PropertyInterface, PW extends PropertyInterface> CalcProperty getPushWhere(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        return getCases().single().where.property;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?, T> pushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, CalcPropertyMapImplement<PW, T> push, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        final ActionCase<PropertyInterface> singleCase = getCases().single();
        return ForActionProperty.pushFor(interfaces, singleCase.where, interfaces.toRevMap(), mapping, context, push, orders, ordersNotNull, new ForActionProperty.PushFor<PropertyInterface, PropertyInterface>() {
            public ActionPropertyMapImplement<?, PropertyInterface> push(ImSet<PropertyInterface> context, CalcPropertyMapImplement<?, PropertyInterface> where, ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders, boolean ordersNotNull, ImRevMap<PropertyInterface, PropertyInterface> mapInnerInterfaces) {
                return createForAction(context, where, orders, ordersNotNull, singleCase.implement.map(mapInnerInterfaces), null, false, SetFact.<PropertyInterface>EMPTY(), false);
            }
        });
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
    public Type getSimpleRequestInputType(boolean optimistic) {
        Type type = null;
        ImList<ActionPropertyMapImplement<?, PropertyInterface>> actions = getListActions();
        for (ActionPropertyMapImplement<?, PropertyInterface> action : actions) {
            Type actionRequestType = action.property.getSimpleRequestInputType(optimistic);
            if (!optimistic && actionRequestType == null) {
                return null;
            }

            if (type == null) {
                type = actionRequestType;
            } else {
                type = type.getCompatible(actionRequestType);
                if (type == null) {
                    return null;
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
        for (ActionPropertyMapImplement<?, PropertyInterface> action : getListActions()) {
            CustomClass simpleAdd = action.property.getSimpleAdd();
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
            for (ActionPropertyMapImplement<?, PropertyInterface> action : getListActions()) {
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

    /*
    public class IfActionProperty extends KeepContextActionProperty {

    private final CalcPropertyMapImplement<?, PropertyInterface> ifProp;
    private final ActionPropertyMapImplement<?, PropertyInterface> trueAction;
    private final ActionPropertyMapImplement<?, PropertyInterface> falseAction;

    public <I extends PropertyInterface> IfActionProperty(String sID, LocalizedString caption, boolean not, ImOrderSet<I> innerInterfaces, CalcPropertyMapImplement<?, I> ifProp, ActionPropertyMapImplement<?, I> trueAction, ActionPropertyMapImplement<?, I> falseAction) {
        super(sID, caption, innerInterfaces.size());

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.ifProp = ifProp.map(mapInterfaces);
        ActionPropertyMapImplement<?, PropertyInterface> mapTrue = trueAction.map(mapInterfaces);
        ActionPropertyMapImplement<?, PropertyInterface> mapFalse = falseAction != null ? falseAction.map(mapInterfaces) : null;
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
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createIfElseUProp(interfaces, ifProp,
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
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        MSet<CalcProperty> used = SetFact.mSet();
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
    public <T extends PropertyInterface, PW extends PropertyInterface> CalcProperty getPushWhere(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        return ifProp.property;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?, T> pushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, CalcPropertyMapImplement<PW, T> push, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        return ForActionProperty.pushFor(interfaces, ifProp, interfaces.toRevMap(), mapping, context, push, orders, ordersNotNull, new ForActionProperty.PushFor<PropertyInterface, PropertyInterface>() {
            public ActionPropertyMapImplement<?, PropertyInterface> push(ImSet<PropertyInterface> context, CalcPropertyMapImplement<?, PropertyInterface> where, ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders, boolean ordersNotNull, ImRevMap<PropertyInterface, PropertyInterface> mapInnerInterfaces) {
                return createForAction(context, where, orders, ordersNotNull, trueAction.map(mapInnerInterfaces), null, false, SetFact.<PropertyInterface>EMPTY(), false);
            }
        });
    }

}
     */
}
