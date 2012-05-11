package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.mergeSet;
import static platform.base.BaseUtils.nullInnerJoin;
import static platform.base.BaseUtils.reverse;

public class JoinActionProperty extends KeepContextActionProperty {

    private final PropertyImplement<ClassPropertyInterface, PropertyInterfaceImplement<ClassPropertyInterface>> action; // action + mapping на calculate

    public <I extends PropertyInterface> JoinActionProperty(String sID, String caption, List<I> listInterfaces, PropertyImplement<ClassPropertyInterface, PropertyInterfaceImplement<I>> implement, ValueClass[] classes) {
        super(sID, caption, classes != null ? classes : getClasses(listInterfaces, implement.mapping.values()));

        action = DerivedProperty.mapImplements(implement, reverse(getMapInterfaces(listInterfaces)));

        finalizeInit();
    }

    public FlowResult flowExecute(ExecutionContext context) throws SQLException {
        Map<ClassPropertyInterface, DataObject> readValues = new HashMap<ClassPropertyInterface, DataObject>();
        for (Map.Entry<ClassPropertyInterface, PropertyInterfaceImplement<ClassPropertyInterface>> mapProp : action.mapping.entrySet()) {
            ObjectValue value = mapProp.getValue().readClasses(context.getSession(), context.getKeys(), context.getModifier());
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
    public Set<Property> getChangeProps() {
        return ((ActionProperty)action.property).getChangeProps();
    }

    @Override
    public Set<Property> getUsedProps() {
        Set<Property> result = new HashSet<Property>(((ActionProperty) action.property).getUsedProps());
        for(PropertyInterfaceImplement<ClassPropertyInterface> value : action.mapping.values())
            value.mapFillDepends(result);
        return result;
    }
}
