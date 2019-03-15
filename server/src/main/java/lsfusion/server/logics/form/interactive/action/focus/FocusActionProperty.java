package lsfusion.server.logics.form.interactive.action.focus;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

// сбрасывает объект в null
public class FocusActionProperty extends SystemExplicitAction {
    private final PropertyDrawEntity property;

    public FocusActionProperty(PropertyDrawEntity property) {
        this.property = property;
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(false, true);
        formInstance.activateProperty(property);
    }
}
