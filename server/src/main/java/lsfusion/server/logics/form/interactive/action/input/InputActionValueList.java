package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class InputActionValueList<P extends PropertyInterface> extends InputValueList<P, Action<P>> {

    public InputActionValueList(Action<P> property, ImMap<P, ObjectValue> mapValues) {
        super(property, mapValues);
    }

    @Override
    public ImSet<Property> getChangeProps() {
        return property.getUsedProps();
    }

    @Override
    protected ImOrderMap<PropertyInterfaceImplement<P>, Boolean> getCacheOrders() {
        return MapFact.EMPTYORDER();
    }

    public P singleInterface() {
        return property.interfaces.removeIncl(mapValues.keys()).single();
    }

    // pretty similar to the InputContextAction.execute
    public void execute(String value, ExecutionContext<?> context) throws SQLException, SQLHandledException {
        property.execute(context.override(MapFact.addExcl(mapValues, singleInterface(), new DataObject(value)), MapFact.EMPTY()));
    }
}
