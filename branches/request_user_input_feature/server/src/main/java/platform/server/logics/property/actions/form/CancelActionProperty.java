package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.server.form.entity.FormEntity;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.session.DataSession;

import java.sql.SQLException;

public class CancelActionProperty extends FormToolbarActionProperty {
    public CancelActionProperty() {
        super("formCancelAction", ApiResourceBundle.getString("form.layout.cancel"), DataSession.isDataChanged, FormEntity.isNewSession);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formCancel(context.getActions());
    }
}
