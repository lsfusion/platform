package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.FormCloseType;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.AnyValuePropertyHolder;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;

public class RequestActionProperty extends AroundAspectActionProperty {
    
    private final Type requestedType; // nullable
    private final LCP requestCanceledProperty;
    private final AnyValuePropertyHolder requestedPropertySet; // needed only when requestedProperty IS NULL


    public <I extends PropertyInterface> RequestActionProperty(LocalizedString caption, ImOrderSet<I> innerInterfaces, ActionPropertyMapImplement<?, I> action,
                                                                        Type requestedType, LCP requestCanceledProperty, AnyValuePropertyHolder requestedPropertySet) {
        super(caption, innerInterfaces, action);

        this.requestedType = requestedType; 
        this.requestCanceledProperty = requestCanceledProperty;
        this.requestedPropertySet = requestedPropertySet;

        finalizeInit();
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue pushedUserInput = context.getPushedUserInput();
        if (pushedUserInput == null) {
            proceed(context);

            if(requestCanceledProperty.read(context) != null) {
                context.setLastUserInput(null);
            } else {
                if(requestedType != null) {
                    context.setLastUserInput(requestedPropertySet.read(requestedType, context));
                } else {
                    ObjectValue value = requestedPropertySet.dropChanges(true, context); // читаем первое не null значение (остальные сбрасываем, чтобы обеспечить инвариант с push)
                    context.setLastUserInput(value);
                }
            }                
        } else {
            if(requestedType != null)
                requestedPropertySet.write(requestedType, pushedUserInput, context);
            else {
                requestedPropertySet.dropChanges(false, context); // сбрасываем все requested
                if(pushedUserInput instanceof DataObject)
                    requestedPropertySet.write(((DataObject) pushedUserInput).getType(), pushedUserInput, context);
            }

            requestCanceledProperty.change((Object)null, context);
        }

        return FlowResult.FINISH;
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        if(requestedType != null)
            return requestedType;
        return aspectActionImplement.property.getSimpleRequestInputType(optimistic, true);
    }
}

