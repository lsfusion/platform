package platform.server.logics.property.actions.flow;

import platform.server.classes.StaticCustomClass;
import platform.server.data.type.Type;
import platform.server.form.instance.FormCloseType;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.List;

public class RequestUserInputActionProperty extends AroundAspectActionProperty {
    private final Type requestValueType;

    private final String chosenKey;

    private final LP requestCanceledProperty;
    private final AnyValuePropertyHolder requestedValueProperty;

    private final AnyValuePropertyHolder chosenValueProperty;

    private final StaticCustomClass formResultClass;
    private final LP formResultProperty;

    public <I extends PropertyInterface> RequestUserInputActionProperty(String sID, String caption, List<I> innerInterfaces, ActionPropertyMapImplement<I> action,
                                                                        Type requestValueType, String chosenKey,
                                                                        LCP requestCanceledProperty, AnyValuePropertyHolder requestedValueProperty,
                                                                        AnyValuePropertyHolder chosenValueProperty, StaticCustomClass formResultClass, LP formResultProperty) {
        super(sID, caption, innerInterfaces, action);

        this.requestValueType = requestValueType;
        this.chosenKey = chosenKey;

        this.requestCanceledProperty = requestCanceledProperty;
        this.requestedValueProperty = requestedValueProperty;

        this.chosenValueProperty = chosenValueProperty;

        this.formResultClass = formResultClass;
        this.formResultProperty = formResultProperty;

        finalizeInit();
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext context) throws SQLException {
        ObjectValue lastUserInput = context.getLastUserInput();
        if (lastUserInput == null) {
            proceed(context);

            if (chosenKey != null) {
                int closeFormResultID = formResultClass.getID(FormCloseType.CLOSE.asString());

                Object value = formResultProperty.read(context);

                if (value != null && !value.equals(closeFormResultID)) {
                    Object chosenValue = chosenValueProperty.read(requestValueType, context, new DataObject(chosenKey));
                    updateRequestedValue(context, chosenValue);
                } else {
                    requestCanceledProperty.change(true, context);
                }
            }
        } else {
            updateRequestedValue(context, lastUserInput.getValue());
        }

        return FlowResult.FINISH;
    }

    private void updateRequestedValue(ExecutionContext context, Object requestedValue) throws SQLException {
        requestedValueProperty.write(requestValueType, requestedValue, context);
        requestCanceledProperty.change(null, context);
    }
}
