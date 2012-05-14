package platform.server.logics.property.actions;

import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.AnyValuePropertyHolder;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class RequestUserDataActionProperty extends CustomActionProperty {

    private final DataClass dataClass;

    private final LP requestCanceledProperty;
    private final AnyValuePropertyHolder requestedValueProperty;

    public RequestUserDataActionProperty(String sID, String caption, DataClass dataClass, LP requestCanceledProperty, AnyValuePropertyHolder requestedValueProperty) {
        super(sID, caption, new ValueClass[0]);

        this.dataClass = dataClass;

        this.requestCanceledProperty = requestCanceledProperty;
        this.requestedValueProperty = requestedValueProperty;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        //todo: init oldValue
        ObjectValue userValue = context.requestUserData(dataClass, null);
        if (userValue == null) {
            requestCanceledProperty.execute(true, context);
        } else {
            requestCanceledProperty.execute(null, context);
            requestedValueProperty.write(dataClass, userValue.getValue(), context);
        }
    }
}
