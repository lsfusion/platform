package platform.server.logics.property.actions.flow;

import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.reverse;

public class JoinActionProperty extends KeepContextActionProperty {

    private final ActionPropertyImplement<CalcPropertyInterfaceImplement<ClassPropertyInterface>> action; // action + mapping на calculate

    public <I extends PropertyInterface> JoinActionProperty(String sID, String caption, List<I> listInterfaces, ActionPropertyImplement<CalcPropertyInterfaceImplement<I>> implement, ValueClass[] classes) {
        super(sID, caption, classes != null ? classes : getClasses(listInterfaces, implement.mapping.values()));

        action = DerivedProperty.mapActionImplements(implement, reverse(getMapInterfaces(listInterfaces)));

        finalizeInit();
    }

    public FlowResult flowExecute(ExecutionContext context) throws SQLException {
        Map<ClassPropertyInterface, DataObject> readValues = new HashMap<ClassPropertyInterface, DataObject>();
        for (Map.Entry<ClassPropertyInterface, CalcPropertyInterfaceImplement<ClassPropertyInterface>> mapProp : action.mapping.entrySet()) {
            ObjectValue value = mapProp.getValue().readClasses(context, context.getKeys());
            if (value instanceof DataObject) {
                readValues.put(mapProp.getKey(), (DataObject) value);
            } else {
                return FlowResult.FINISH;
            }
        }
        ((ActionProperty) action.property).execute(context.override(readValues, action.mapping));
        return FlowResult.FINISH;
    }

    @Override
    public Set<CalcProperty> getChangeProps() {
        return ((ActionProperty)action.property).getChangeProps();
    }

    public Set<CalcProperty> getUsedProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>(((ActionProperty) action.property).getUsedProps());
        for(CalcPropertyInterfaceImplement<ClassPropertyInterface> value : action.mapping.values())
            value.mapFillDepends(result);
        return result;
    }
}
