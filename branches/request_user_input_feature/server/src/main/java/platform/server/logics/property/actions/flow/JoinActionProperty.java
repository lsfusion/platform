package platform.server.logics.property.actions.flow;

import platform.server.caches.IdentityLazy;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.reverse;

public class JoinActionProperty<T extends PropertyInterface> extends KeepContextActionProperty {

    private final ActionPropertyImplement<T, CalcPropertyInterfaceImplement<PropertyInterface>> action; // action + mapping на calculate

    public <I extends PropertyInterface> JoinActionProperty(String sID, String caption, List<I> listInterfaces, ActionPropertyImplement<T, CalcPropertyInterfaceImplement<I>> implement, ValueClass[] classes) {
        super(sID, caption, listInterfaces.size());

        action = DerivedProperty.mapActionImplements(implement, reverse(getMapInterfaces(listInterfaces)));

        finalizeInit();
    }

    public FlowResult execute(ExecutionContext<PropertyInterface> context) throws SQLException {
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
    public Set<CalcProperty> getChangeProps() {
        return action.property.getChangeProps();
    }

    public Set<CalcProperty> getUsedProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>(action.property.getUsedProps());
        for(CalcPropertyInterfaceImplement<PropertyInterface> value : action.mapping.values())
            value.mapFillDepends(result);
        return result;
    }

    @IdentityLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return DerivedProperty.createJoin(action.property.getWhereProperty().mapImplement(action.mapping));
    }
}
