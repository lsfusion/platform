package platform.server.logics.property.actions.flow;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.classes.CustomClass;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class JoinActionProperty<T extends PropertyInterface> extends KeepContextActionProperty {

    public final ActionPropertyImplement<T, CalcPropertyInterfaceImplement<PropertyInterface>> action; // action + mapping на calculate

    public <I extends PropertyInterface> JoinActionProperty(String sID, String caption, ImOrderSet<I> listInterfaces, ActionPropertyImplement<T, CalcPropertyInterfaceImplement<I>> implement) {
        super(sID, caption, listInterfaces.size());

        action = DerivedProperty.mapActionImplements(implement, getMapInterfaces(listInterfaces).reverse());

        finalizeInit();
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        ImFilterValueMap<T, DataObject> mvReadValues = action.mapping.mapFilterValues();
        for (int i=0,size=action.mapping.size();i<size;i++) {
            ObjectValue value = action.mapping.getValue(i).readClasses(context, context.getKeys());
            if (value instanceof DataObject) {
                mvReadValues.mapValue(i, (DataObject) value);
            } else {
                return FlowResult.FINISH;
            }
        }
        action.property.execute(context.override(mvReadValues.immutableValue(), action.mapping));
        return FlowResult.FINISH;
    }

    @Override
    public Type getSimpleRequestInputType() {
        return action.property.getSimpleRequestInputType();
    }

    @Override
    public CustomClass getSimpleAdd() {
        return action.property.getSimpleAdd();
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        T simpleRemove = action.property.getSimpleDelete();
        CalcPropertyInterfaceImplement<PropertyInterface> mapRemove;
        if(simpleRemove!=null && ((mapRemove = action.mapping.get(simpleRemove)) instanceof PropertyInterface))
            return (PropertyInterface) mapRemove;
        return null;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type != ChangeFlowType.RETURN && super.hasFlow(type);
    }

    public ImSet<ActionProperty> getDependActions() {
        return SetFact.singleton((ActionProperty)action.property);
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        MSet<CalcProperty> used = SetFact.mSet();
        for(CalcPropertyInterfaceImplement<PropertyInterface> value : action.mapping.valueIt())
            value.mapFillDepends(used);
        return used.immutable().toMap(false).merge(super.aspectUsedExtProps(), addValue);
    }

    @IdentityInstanceLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createJoin(action.property.getWhereProperty().mapImplement(action.mapping));
    }

    @Override
    public ImList<ActionPropertyMapImplement<?, PropertyInterface>> getList() {
        // если все интерфейсы однозначны и нет return'ов - inlin'им
        if(action.property.hasFlow(ChangeFlowType.RETURN))
            return super.getList();
        
        ImRevMap<T, PropertyInterface> identityMap = PropertyInterface.getIdentityMap(action.mapping);
        if(identityMap == null)
            return super.getList();

        return DerivedProperty.mapActionImplements(identityMap, action.property.getList());
    }
}
