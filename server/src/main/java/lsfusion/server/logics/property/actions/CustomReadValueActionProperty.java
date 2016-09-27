package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public abstract class CustomReadValueActionProperty extends UserActionProperty {

    protected CustomReadValueActionProperty(LocalizedString caption, ValueClass[] classes) {
        super(caption, classes);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Object userValue = null;

        DataClass readType = getReadType();
        if (readType != null) {
            ObjectValue objectValue = context.requestUserData(readType, null);
            if (objectValue == null) {
                return;
            }

            userValue = objectValue.getValue();
        }

        executeRead(context, userValue);
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic) {
        return getReadType();
    }

    protected abstract void executeRead(ExecutionContext<ClassPropertyInterface> context, Object userValue) throws SQLException, SQLHandledException;

    protected abstract DataClass getReadType();
}
