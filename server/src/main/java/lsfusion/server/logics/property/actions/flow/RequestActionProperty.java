package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;

public class RequestActionProperty extends AroundAspectActionProperty {
    
    private final Type requestedType; // nullable


    public <I extends PropertyInterface> RequestActionProperty(LocalizedString caption, ImOrderSet<I> innerInterfaces, ActionPropertyMapImplement<?, I> action,
                                                               Type requestedType) {
        super(caption, innerInterfaces, action);

        this.requestedType = requestedType; 

        finalizeInit();
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        boolean isRequest = context.isRequest();
        if (!isRequest)
            proceed(context);
        return FlowResult.FINISH;
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        return aspectActionImplement.property.getSimpleRequestInputType(optimistic, true);
    }
}

