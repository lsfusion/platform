package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.server.form.entity.FormEntity;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class NullActionProperty extends FormToolbarActionProperty {

    public NullActionProperty() {
        super("formNull", ApiResourceBundle.getString("form.layout.reset"), null, FormEntity.isDialog);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formNull();
    }
}
