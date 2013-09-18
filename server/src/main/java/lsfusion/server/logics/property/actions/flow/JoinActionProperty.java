package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class JoinActionProperty<T extends PropertyInterface> extends KeepContextActionProperty {

    public final ActionPropertyImplement<T, CalcPropertyInterfaceImplement<PropertyInterface>> action; // action + mapping на calculate

    public <I extends PropertyInterface> JoinActionProperty(String sID, String caption, ImOrderSet<I> listInterfaces, ActionPropertyImplement<T, CalcPropertyInterfaceImplement<I>> implement) {
        super(sID, caption, listInterfaces.size());

        action = DerivedProperty.mapActionImplements(implement, getMapInterfaces(listInterfaces).reverse());

        finalizeInit();
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        ImFilterValueMap<T, ObjectValue> mvReadValues = action.mapping.mapFilterValues();
        for (int i=0,size=action.mapping.size();i<size;i++)
            mvReadValues.mapValue(i, action.mapping.getValue(i).readClasses(context, context.getKeys()));
        action.property.execute(context.override(mvReadValues.immutableValue(), action.mapping));
        return FlowResult.FINISH;
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic) {
        if(isRecursive) // recursion guard
            return null;
        return action.property.getSimpleRequestInputType(optimistic);
    }

    @Override
    public CustomClass getSimpleAdd() {
        if(isRecursive) // recursion guard
            return null;
        return action.property.getSimpleAdd();
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        if(isRecursive) // recursion guard
            return null;

        T simpleRemove = action.property.getSimpleDelete();
        CalcPropertyInterfaceImplement<PropertyInterface> mapRemove;
        if(simpleRemove!=null && ((mapRemove = action.mapping.get(simpleRemove)) instanceof PropertyInterface))
            return (PropertyInterface) mapRemove;
        return null;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(isRecursive) // recursion guard
            return false;

        return type != ChangeFlowType.RETURN && super.hasFlow(type);
    }

    public ImSet<ActionProperty> getDependActions() {
        return SetFact.singleton((ActionProperty)action.property);
    }

    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        if(isRecursive) // recursion guard
            return MapFact.EMPTY();
        return super.aspectChangeExtProps();
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        MSet<CalcProperty> used = SetFact.mSet();
        for(CalcPropertyInterfaceImplement<PropertyInterface> value : action.mapping.valueIt())
            value.mapFillDepends(used);
        ImMap<CalcProperty, Boolean> result = used.immutable().toMap(false);
        if(!isRecursive)
            result = result.merge(super.aspectUsedExtProps(), addValue);
        return result;
    }

    @IdentityInstanceLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() { // тут на recursive не смо
        return DerivedProperty.createJoin(action.property.getWhereProperty().mapImplement(action.mapping));
    }

    @Override
    public ImList<ActionPropertyMapImplement<?, PropertyInterface>> getList() {
        // если все интерфейсы однозначны и нет return'ов - inlin'им
        if(isRecursive || action.property.hasFlow(ChangeFlowType.RETURN))
            return super.getList();
        
        ImRevMap<T, PropertyInterface> identityMap = PropertyInterface.getIdentityMap(action.mapping);
        if(identityMap == null)
            return super.getList();

        return DerivedProperty.mapActionImplements(identityMap, action.property.getList());
    }

    private boolean isRecursive;
    @Override
    protected void markRecursions(ListCaseActionProperty recursiveAction) {
        if(BaseUtils.hashEquals(action.property, recursiveAction)) {
            assert recursiveAction.isAbstract();
            isRecursive = true;
        } else
            super.markRecursions(recursiveAction);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
