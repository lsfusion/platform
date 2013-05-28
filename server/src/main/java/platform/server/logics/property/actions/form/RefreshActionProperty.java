package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class RefreshActionProperty extends FormToolbarActionProperty {

    public RefreshActionProperty() {
        super("formRefresh", ApiResourceBundle.getString("form.layout.refresh"), false);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formRefresh();
    }
}
