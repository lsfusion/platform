package lsfusion.server.logics.property.actions.form;

import lsfusion.base.ApiResourceBundle;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class RefreshActionProperty extends FormToolbarActionProperty {

    public RefreshActionProperty() {
        super("formRefresh", ApiResourceBundle.getString("form.layout.refresh"), false);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formRefresh();
    }
}
