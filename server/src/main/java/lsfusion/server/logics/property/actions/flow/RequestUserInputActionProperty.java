package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.FormCloseType;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;

import java.sql.SQLException;

public class RequestUserInputActionProperty extends AroundAspectActionProperty {
    private final Type requestValueType;

    private final String chosenKey;

    private final AnyValuePropertyHolder chosenValueProperty;

    private final ConcreteCustomClass formResultClass;
    private final LCP formResultProperty;

    public <I extends PropertyInterface> RequestUserInputActionProperty(LocalizedString caption, ImOrderSet<I> innerInterfaces, ActionPropertyMapImplement<?, I> action,
                                                                        Type requestValueType, String chosenKey,
                                                                        AnyValuePropertyHolder chosenValueProperty, ConcreteCustomClass formResultClass, LCP formResultProperty) {
        super(caption, innerInterfaces, action);

        this.requestValueType = requestValueType;
        this.chosenKey = chosenKey;

        this.chosenValueProperty = chosenValueProperty;

        this.formResultClass = formResultClass;
        this.formResultProperty = formResultProperty;

        finalizeInit();
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        boolean isRequest = context.isRequest();
        if (!isRequest) {
            proceed(context);

            if (chosenKey != null) {
                int closeFormResultID = formResultClass.getObjectID(FormCloseType.CLOSE.asString());
                int dropFormResultID = formResultClass.getObjectID(FormCloseType.DROP.asString());

                Object value = formResultProperty.read(context);
                ObjectValue chosenValue = null;
                if (value != null && !value.equals(closeFormResultID)) // CLOSE
                    chosenValue = value.equals(dropFormResultID) ? NullValue.instance : chosenValueProperty.read(requestValueType, context, new DataObject(chosenKey)); // DROP / OK 
                context.writeRequested(RequestResult.get(chosenValue, requestValueType, null));
            }
        } 

        return FlowResult.FINISH;
    }

}
