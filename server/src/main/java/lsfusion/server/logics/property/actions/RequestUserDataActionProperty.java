package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.DataClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class RequestUserDataActionProperty extends SystemExplicitActionProperty {

    private final DataClass dataClass;

    public RequestUserDataActionProperty(LocalizedString caption, DataClass dataClass) {
        super(caption);

        this.dataClass = dataClass;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.requestUserData(dataClass, null);
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        return dataClass;
    }
}
