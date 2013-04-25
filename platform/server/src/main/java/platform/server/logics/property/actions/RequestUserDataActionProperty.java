package platform.server.logics.property.actions;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.DataClass;
import platform.server.data.type.Type;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.AnyValuePropertyHolder;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class RequestUserDataActionProperty extends SystemActionProperty {

    private final DataClass dataClass;

    private final LCP<?> requestCanceledProperty;
    private final AnyValuePropertyHolder requestedValueProperty;

    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getChangeProps(requestCanceledProperty.property, requestedValueProperty.getLCP(dataClass).property);
    }

    public RequestUserDataActionProperty(String sID, String caption, DataClass dataClass, LCP requestCanceledProperty, AnyValuePropertyHolder requestedValueProperty) {
        super(sID, caption);

        this.dataClass = dataClass;

        this.requestCanceledProperty = requestCanceledProperty;
        this.requestedValueProperty = requestedValueProperty;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
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
    public Type getSimpleRequestInputType() {
        return dataClass;
    }
}
