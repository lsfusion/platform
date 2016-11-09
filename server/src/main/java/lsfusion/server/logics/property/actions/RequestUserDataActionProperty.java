package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.AnyValuePropertyHolder;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class RequestUserDataActionProperty extends SystemExplicitActionProperty {

    private final DataClass dataClass;

    private final LCP<?> requestCanceledProperty;
    private final AnyValuePropertyHolder requestedValueProperty;

    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getChangeProps(requestCanceledProperty.property, requestedValueProperty.getLCP(dataClass).property);
    }

    public RequestUserDataActionProperty(String caption, DataClass dataClass, LCP requestCanceledProperty, AnyValuePropertyHolder requestedValueProperty) {
        super(caption);

        this.dataClass = dataClass;

        this.requestCanceledProperty = requestCanceledProperty;
        this.requestedValueProperty = requestedValueProperty;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        //todo: init oldValue
        ObjectValue userValue = context.requestUserData(dataClass, null);
        if (userValue == null) {
            requestCanceledProperty.change(true, context);
        } else {
            requestCanceledProperty.change((Object)null, context);
            requestedValueProperty.write(dataClass, userValue, context);
        }
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic) {
        return dataClass;
    }
}
