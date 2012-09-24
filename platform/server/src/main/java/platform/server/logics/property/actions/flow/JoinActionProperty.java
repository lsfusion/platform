package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.classes.CustomClass;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.reverse;

public class JoinActionProperty<T extends PropertyInterface> extends KeepContextActionProperty {

    public final ActionPropertyImplement<T, CalcPropertyInterfaceImplement<PropertyInterface>> action; // action + mapping на calculate

    public <I extends PropertyInterface> JoinActionProperty(String sID, String caption, List<I> listInterfaces, ActionPropertyImplement<T, CalcPropertyInterfaceImplement<I>> implement) {
        super(sID, caption, listInterfaces.size());

        action = DerivedProperty.mapActionImplements(implement, reverse(getMapInterfaces(listInterfaces)));

        finalizeInit();
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        Map<T, DataObject> readValues = new HashMap<T, DataObject>();
        for (Map.Entry<T, CalcPropertyInterfaceImplement<PropertyInterface>> mapProp : action.mapping.entrySet()) {
            ObjectValue value = mapProp.getValue().readClasses(context, context.getKeys());
            if (value instanceof DataObject) {
                readValues.put(mapProp.getKey(), (DataObject) value);
            } else {
                return FlowResult.FINISH;
            }
        }
        action.property.execute(context.override(readValues, action.mapping));
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

    public Set<ActionProperty> getDependActions() {
        return Collections.singleton((ActionProperty)action.property);
    }

    @Override
    public Set<CalcProperty> getUsedProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        for(CalcPropertyInterfaceImplement<PropertyInterface> value : action.mapping.values())
            value.mapFillDepends(result);
        result.addAll(super.getUsedProps());
        return result;
    }

    @IdentityLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createJoin(action.property.getWhereProperty().mapImplement(action.mapping));
    }

    @Override
    public List<ActionPropertyMapImplement<?, PropertyInterface>> getList() {
        // если все интерфейсы однозначны и нет return'ов - inlin'им
        if(action.property.hasFlow(ChangeFlowType.RETURN))
            return super.getList();
        
        Set<PropertyInterface> checked = new HashSet<PropertyInterface>();
        for(CalcPropertyInterfaceImplement<PropertyInterface> propImplement : action.mapping.values())
            if(!(propImplement instanceof PropertyInterface && checked.add((PropertyInterface) propImplement)))
                return super.getList();

        return DerivedProperty.mapActionImplements(BaseUtils.<Map<T, PropertyInterface>>immutableCast(action.mapping), action.property.getList());
    }
}
