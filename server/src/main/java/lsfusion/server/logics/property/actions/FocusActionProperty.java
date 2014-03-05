package lsfusion.server.logics.property.actions;

import lsfusion.interop.action.FocusClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

// сбрасывает объект в null
public class FocusActionProperty extends SystemExplicitActionProperty {
    private final int propertyId;

    public FocusActionProperty(String sID, int propertyId) {
        super(sID);
        this.propertyId = propertyId;
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.requestUserInteraction(new FocusClientAction(propertyId));
    }
}
