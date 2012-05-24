package platform.server.logics.property.actions.form;

import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomActionProperty;

import java.sql.SQLException;

public class RefreshActionProperty extends CustomActionProperty {

    public RefreshActionProperty() {
//        super("formRefreshAction", ApiResourceBundle.getString("form.layout.refresh"), new ValueClass[0]);
        super("formRefreshAction", "", new ValueClass[0]);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formRefresh(context.getActions());
    }
}
