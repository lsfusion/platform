package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class ListActionProperty extends ListCaseActionProperty {

    private Object actions;
    public void addAction(ActionPropertyMapImplement<?, PropertyInterface> action) {
        ((MList<ActionPropertyMapImplement<?, PropertyInterface>>)actions).add(action);

        addWhereOperand(action);
    }

    private ImList<ActionPropertyMapImplement<?, PropertyInterface>> getActions() {
        return (ImList<ActionPropertyMapImplement<?, PropertyInterface>>)actions;
    }

    // так, а не как в Join'е, потому как нужны ClassPropertyInterface'ы а там нужны классы
    public <I extends PropertyInterface> ListActionProperty(String sID, String caption, ImOrderSet<I> innerInterfaces, ImList<ActionPropertyMapImplement<?, I>> actions)  {
        super(sID, caption, false, innerInterfaces);

        this.actions = DerivedProperty.mapActionImplements(getMapInterfaces(innerInterfaces).reverse(), actions);

        finalizeInit();
    }

    public <I extends PropertyInterface> ListActionProperty(String sID, String caption, boolean isChecked, ImOrderSet<I> innerInterfaces, ImMap<I, ValueClass> mapClasses)  {
        super(sID, caption, false, isChecked, AbstractType.LIST, innerInterfaces, mapClasses);

        actions = ListFact.mList();
    }

    public CalcPropertyMapImplement<?, PropertyInterface> calculateWhereProperty() {

        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listWheres = getActions().mapListValues(new GetValue<CalcPropertyInterfaceImplement<PropertyInterface>, ActionPropertyMapImplement<?, PropertyInterface>>() {
            public CalcPropertyInterfaceImplement<PropertyInterface> getMapValue(ActionPropertyMapImplement<?, PropertyInterface> value) {
                return value.mapWhereProperty();
            }});
        return DerivedProperty.createUnion(interfaces, listWheres);
    }

    protected ImList<ActionPropertyMapImplement<?, PropertyInterface>> getListActions() {
        return getActions();
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        FlowResult result = FlowResult.FINISH;

        for (ActionPropertyMapImplement<?, PropertyInterface> action : getActions()) {
            FlowResult actionResult = action.execute(context);
            if (actionResult != FlowResult.FINISH) {
                result =  actionResult;
                break;
            }
        }

        return result;
    }

    @Override
    public void finalizeInit() {
        super.finalizeInit();

        if(isAbstract())
            actions = ((MList<ActionPropertyMapImplement<?, PropertyInterface>>)actions).immutableList();
    }

    @Override
    public ImList<ActionPropertyMapImplement<?, PropertyInterface>> getList() {
        MList<ActionPropertyMapImplement<?, PropertyInterface>> mResult = ListFact.mList();
        for(ActionPropertyMapImplement<?, PropertyInterface> action : getActions())
            mResult.addAll(action.getList());
        return mResult.immutableList();
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic) {
        Type type = null;
        for (ActionPropertyMapImplement<?, PropertyInterface> action : getListActions()) {
            Type actionRequestType = action.property.getSimpleRequestInputType(optimistic);
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
                    return null;
                }
            }
        }
        return result;
    }
}
