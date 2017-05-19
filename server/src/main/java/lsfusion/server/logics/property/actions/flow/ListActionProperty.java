package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.debug.ActionDelegationType;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.impl.NFListImpl;
import lsfusion.server.logics.mutables.interfaces.NFList;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class ListActionProperty extends ListCaseActionProperty {

    private Object actions;
    public void addAction(ActionPropertyMapImplement<?, PropertyInterface> action, Version version) {
        assert action != null;
        NFListImpl.add(isLast, (NFList<ActionPropertyMapImplement<?, PropertyInterface>>) actions, action, version);

        addWhereOperand(action, null, version);
    }

    public ImList<ActionPropertyMapImplement<?, PropertyInterface>> getActions() {
        return (ImList<ActionPropertyMapImplement<?, PropertyInterface>>)actions;
    }

    private final ImSet<SessionDataProperty> localsInScope;

    // так, а не как в Join'е, потому как нужны ClassPropertyInterface'ы а там нужны классы
    public <I extends PropertyInterface> ListActionProperty(LocalizedString caption, ImOrderSet<I> innerInterfaces, ImList<ActionPropertyMapImplement<?, I>> actions, ImSet<SessionDataProperty> localsInScope)  {
        super(caption, false, innerInterfaces);

        this.actions = DerivedProperty.mapActionImplements(getMapInterfaces(innerInterfaces).reverse(), actions);
        this.localsInScope = localsInScope;

        finalizeInit();
    }

    // abstract конструктор без finalize'а
    public <I extends PropertyInterface> ListActionProperty(LocalizedString caption, boolean isChecked, boolean isLast, ImOrderSet<I> innerInterfaces, ImMap<I, ValueClass> mapClasses)  {
        super(caption, false, isChecked, isLast, AbstractType.LIST, innerInterfaces, mapClasses);

        actions = NFFact.list();
        localsInScope = SetFact.EMPTY();
    }

    public CalcPropertyMapImplement<?, PropertyInterface> calcCaseWhereProperty() {

        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listWheres = getActions().mapListValues(new GetValue<CalcPropertyInterfaceImplement<PropertyInterface>, ActionPropertyMapImplement<?, PropertyInterface>>() {
            public CalcPropertyInterfaceImplement<PropertyInterface> getMapValue(ActionPropertyMapImplement<?, PropertyInterface> value) {
                return value.mapCalcWhereProperty();
            }});
        return DerivedProperty.createUnion(interfaces, listWheres);
    }

    protected ImList<ActionPropertyMapImplement<?, PropertyInterface>> getListActions() {
        return getActions();
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        FlowResult result = FlowResult.FINISH;

        for (ActionPropertyMapImplement<?, PropertyInterface> action : getActions()) {
            FlowResult actionResult = action.execute(context);
            if (actionResult != FlowResult.FINISH) {
                result =  actionResult;
                break;
            }
        }

        context.getSession().dropSessionChanges(localsInScope);

        return result;
    }

    @Override
    protected void finalizeAbstractInit() {
        super.finalizeAbstractInit();
        
        actions = ((NFList<ActionPropertyMapImplement<?, PropertyInterface>>)actions).getList();
    }

    @Override
    public ImList<ActionPropertyMapImplement<?, PropertyInterface>> getList() {
        MList<ActionPropertyMapImplement<?, PropertyInterface>> mResult = ListFact.mList();
        for(ActionPropertyMapImplement<?, PropertyInterface> action : getActions())
            mResult.addAll(action.getList());
        return mResult.immutableList();
    }

    @Override
    public Type getFlowSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        Type type = null;
        for (ActionPropertyMapImplement<?, PropertyInterface> action : getListActions()) {
            Type actionRequestType = action.property.getSimpleRequestInputType(optimistic, inRequest);
            if (actionRequestType != null) {
                if (type == null) {
                    type = actionRequestType;
                } else {
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
        CustomClass result = null;
        for (ActionPropertyMapImplement<?, PropertyInterface> action : getListActions()) {
            CustomClass simpleAdd = action.property.getSimpleAdd();
            if (simpleAdd != null) {
                if (result == null) {
                    result = simpleAdd;
                } else {
                    return null;
                }
            }
        }
        return result;
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        PropertyInterface result = null;
        for (ActionPropertyMapImplement<?, PropertyInterface> action : getListActions()) {
            PropertyInterface simpleDelete = action.mapSimpleDelete();
            if (simpleDelete != null) {
                if (result == null) {
                    result = simpleDelete;
                } else {
                    result = null;
                    break;
                }
            }
        }
        if(result != null)
            return result;
        return super.getSimpleDelete();
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        if(modifyContext || hasDebugLocals())
            return ActionDelegationType.BEFORE_DELEGATE;
        else
            return null;
    }

    // пока просто ищем в конце APPLY и CHANGE'ы после APPLY
    // потом по хорошему надо будет в if then apply else cancel 
    private boolean hasApplyWithNoChangeAfter() {

        ImList<ActionPropertyMapImplement<?, PropertyInterface>> actions = getActions();
        for(int i = actions.size() - 1; i>= 0; i--) {
            ActionProperty<?> listAction = actions.get(i).property;
            while(listAction instanceof JoinActionProperty)
                listAction = ((JoinActionProperty) listAction).action.property;

            if(listAction instanceof ApplyActionProperty)
                return true;
            
            if(listAction.hasFlow(ChangeFlowType.CHANGE)) {
                return listAction instanceof ListActionProperty && ((ListActionProperty) listAction).hasApplyWithNoChangeAfter();
            }
        }
        
        assert false; // так как сверщу есит проверка на hasFlow(CHANGE)
        return false;
    }
    
    @Override
    public boolean hasFlow(ChangeFlowType type) {
        boolean hasFlow = super.hasFlow(type);
        
        if(type == ChangeFlowType.CHANGE && hasFlow) {
            return !hasApplyWithNoChangeAfter();
        }
        
        return hasFlow;
    }
}
