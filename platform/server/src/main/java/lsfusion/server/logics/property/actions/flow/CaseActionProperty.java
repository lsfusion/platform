package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.Settings;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.OrObjectClassSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

import static lsfusion.server.logics.property.derived.DerivedProperty.createForAction;

public class CaseActionProperty extends ListCaseActionProperty {

    private Object cases;
    private final boolean caseClasses;

    public static <I extends PropertyInterface> ActionProperty createIf(String sID, String caption, boolean not, ImOrderSet<I> innerInterfaces, CalcPropertyMapImplement<?, I> ifProp, ActionPropertyMapImplement<?, I> trueAction, ActionPropertyMapImplement<?, I> falseAction) {
        assert trueAction != null;
        if(not) // просто not'им if
            ifProp = DerivedProperty.createNot(ifProp);

        MList<Case<I>> mCases = ListFact.mListMax(2);
        mCases.add(new Case<I>(ifProp, trueAction));
        if(falseAction != null)
            mCases.add(new Case<I>(DerivedProperty.<I>createTrue(), falseAction));
        return new CaseActionProperty(sID, caption, false, innerInterfaces, mCases.immutableList());
    }

    public static class Case<P extends PropertyInterface> {
        public final CalcPropertyMapImplement<?, P> where;
        public final ActionPropertyMapImplement<?, P> action;

        public Case(CalcPropertyMapImplement<?, P> where, ActionPropertyMapImplement<?, P> action) {
            this.where = where;
            this.action = action;
        }
    }

    public void addCase(CalcPropertyMapImplement<?, PropertyInterface> where, ActionPropertyMapImplement<?, PropertyInterface> action) {
        assert !caseClasses;

        addCase(new Case<PropertyInterface>(where, action));
    }

    public void addOperand(ActionPropertyMapImplement<?,PropertyInterface> action) {
        assert isAbstract();

        CalcPropertyMapImplement<?, PropertyInterface> where = action.mapWhereProperty();
        Case<PropertyInterface> addCase;
        if(caseClasses)
            addCase = new Case<PropertyInterface>((CalcPropertyMapImplement<?,PropertyInterface>) where.mapClassProperty(), action);
        else
            addCase = new Case<PropertyInterface>(where, action);

        addCase(addCase);
    }

    public void addCase(Case<PropertyInterface> aCase) {
        ((MList<Case<PropertyInterface>>)cases).add(aCase);

        addWhereCase(aCase.where, aCase.action);
    }

    private ImList<Case<PropertyInterface>> getCases() {
        return (ImList<Case<PropertyInterface>>)cases;
    }

    public <I extends PropertyInterface> CaseActionProperty(String sID, String caption, boolean isExclusive, ImList<ActionPropertyMapImplement> impls, ImOrderSet<I> innerInterfaces) {
        this(sID, caption, isExclusive, innerInterfaces, impls.<Case<I>>mapListValues(new GetValue<Case<I>, ActionPropertyMapImplement>() {
            @Override
            public Case<I> getMapValue(ActionPropertyMapImplement value) {
                return new Case<I>((CalcPropertyMapImplement)value.mapWhereProperty().mapClassProperty(), value);
            }
        }));
    }

    public <I extends PropertyInterface> CaseActionProperty(String sID, String caption, boolean isExclusive, ImOrderSet<I> innerInterfaces, ImList<Case<I>> cases)  {
        super(sID, caption, isExclusive, innerInterfaces);

        final ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.cases = cases.mapListValues(new GetValue<Case<PropertyInterface>, Case<I>>() {
            public Case<PropertyInterface> getMapValue(Case<I> value) {
                return new Case<PropertyInterface>(value.where.map(mapInterfaces), value.action.map(mapInterfaces));
            }});
        caseClasses = false;

        finalizeInit();
    }

    public <I extends PropertyInterface> CaseActionProperty(String sID, String caption, boolean isExclusive, boolean caseClasses, ImOrderSet<I> innerInterfaces, ImMap<I, ValueClass> mapClasses)  {
        super(sID, caption, isExclusive, innerInterfaces, mapClasses);

        cases = ListFact.mList();
        this.caseClasses = caseClasses;
    }

    protected CalcPropertyMapImplement<?, PropertyInterface> calculateWhereProperty() {
        ImList<Pair<CalcPropertyInterfaceImplement<PropertyInterface>, CalcPropertyInterfaceImplement<PropertyInterface>>> listWheres = getCases().mapListValues(new GetValue<Pair<CalcPropertyInterfaceImplement<PropertyInterface>, CalcPropertyInterfaceImplement<PropertyInterface>>, Case<PropertyInterface>>() {
            public Pair<CalcPropertyInterfaceImplement<PropertyInterface>, CalcPropertyInterfaceImplement<PropertyInterface>> getMapValue(Case<PropertyInterface> value) {
                return new Pair<CalcPropertyInterfaceImplement<PropertyInterface>, CalcPropertyInterfaceImplement<PropertyInterface>>(value.where, value.action.mapWhereProperty());
            }});
        return DerivedProperty.createUnion(interfaces, isExclusive, listWheres);
    }

    protected ImList<ActionPropertyMapImplement<?, PropertyInterface>> getListActions() {
        return getCases().mapListValues(new GetValue<ActionPropertyMapImplement<?, PropertyInterface>, Case<PropertyInterface>>() {
            public ActionPropertyMapImplement<?, PropertyInterface> getMapValue(Case<PropertyInterface> value) {
                return value.action;
            }});
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        return getCases().mapListValues(new GetValue<CalcProperty, Case<PropertyInterface>>() {
            public CalcProperty getMapValue(Case<PropertyInterface> value) {
                return value.where.property;
            }}).toOrderSet().getSet().toMap(false).merge(super.aspectUsedExtProps(), addValue);
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        FlowResult result = FlowResult.FINISH;
        for(Case<PropertyInterface> aCase : getCases()) {
            if(aCase.where.read(context, context.getKeys()) != null) {
                result = aCase.action.execute(context);
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

        final Case<PropertyInterface> singleCase = getCases().single();
        return ForActionProperty.pushFor(interfaces, singleCase.where, interfaces.toRevMap(), mapping, context, push, orders, ordersNotNull, new ForActionProperty.PushFor<PropertyInterface, PropertyInterface>() {
            public ActionPropertyMapImplement<?, PropertyInterface> push(ImSet<PropertyInterface> context, CalcPropertyMapImplement<?, PropertyInterface> where, ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders, boolean ordersNotNull, ImRevMap<PropertyInterface, PropertyInterface> mapInnerInterfaces) {
                return createForAction(context, where, orders, ordersNotNull, singleCase.action.map(mapInnerInterfaces), null, false, SetFact.<PropertyInterface>EMPTY(), false);
            }
        });
    }

    @Override
    public void finalizeInit() {
        super.finalizeInit();

        if(isAbstract())
            cases = ((MList<Case<PropertyInterface>>)cases).immutableList();
    }

    @Override
    public Type getSimpleRequestInputType() {
        Type type = null;
        ImList<ActionPropertyMapImplement<?, PropertyInterface>> actions = getListActions();
        for (ActionPropertyMapImplement<?, PropertyInterface> action : actions) {
            Type actionRequestType = action.property.getSimpleRequestInputType();
//            if (actionRequestType == null) // не будем пока блокировать асинхронное выполнение, так как по сравнению с simpleAdd это не настолько часто заметно
//                return null;

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

        if(!isExclusive && Settings.get().isDisableSimpleAddRemoveInNonExclCase())
            return null;

        PropertyInterface result = null;
        for (ActionPropertyMapImplement<?, PropertyInterface> action : getListActions()) {
            PropertyInterface simpleDelete = action.mapSimpleDelete();
            if(simpleDelete!=null && (result==null || BaseUtils.hashEquals(result, simpleDelete)))
                result = simpleDelete;
            else // значит есть case который не удаляет или удаляет что-то другое
                return null;
        }
        return result;
    }

    /*
    public class IfActionProperty extends KeepContextActionProperty {

    private final CalcPropertyMapImplement<?, PropertyInterface> ifProp;
    private final ActionPropertyMapImplement<?, PropertyInterface> trueAction;
    private final ActionPropertyMapImplement<?, PropertyInterface> falseAction;

    public <I extends PropertyInterface> IfActionProperty(String sID, String caption, boolean not, ImOrderSet<I> innerInterfaces, CalcPropertyMapImplement<?, I> ifProp, ActionPropertyMapImplement<?, I> trueAction, ActionPropertyMapImplement<?, I> falseAction) {
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
