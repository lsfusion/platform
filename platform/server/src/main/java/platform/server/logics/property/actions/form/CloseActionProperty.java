package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.server.form.entity.FormEntity;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class CloseActionProperty extends FormToolbarActionProperty {
    public CloseActionProperty() {
        super("formClose", ApiResourceBundle.getString("form.layout.close"), null, FormEntity.isModal);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formClose();
    }
}
