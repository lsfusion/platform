package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.server.form.entity.FormEntity;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class OkActionProperty extends FormToolbarActionProperty {

    public OkActionProperty() {
        super("formOk", ApiResourceBundle.getString("form.layout.ok"), null, FormEntity.isModal);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formOk();
    }
}
