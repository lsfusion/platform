package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.interop.action.RunEditReportClientAction;
import platform.server.form.entity.FormEntity;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class EditActionProperty extends FormToolbarActionProperty {

    public EditActionProperty() {
        super("formEdit", ApiResourceBundle.getString("form.layout.edit"), false, null,
              new CalcProperty[] {FormEntity.isFullClient, FormEntity.isDebug, FormEntity.isDialog}, new boolean[] {false, false, true});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.delayUserInterfaction(new RunEditReportClientAction());
    }
}
