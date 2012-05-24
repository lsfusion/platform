package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomActionProperty;

import java.sql.SQLException;

public class CancelActionProperty extends CustomActionProperty {
    public CancelActionProperty() {
        super("formCancelAction", ApiResourceBundle.getString("form.layout.cancel"), new ValueClass[0]);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formCancel(context.getActions());
    }
}
