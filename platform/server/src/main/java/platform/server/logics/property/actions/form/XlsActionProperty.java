package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.interop.action.RunOpenInExcelClientAction;
import platform.server.form.entity.FormEntity;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class XlsActionProperty extends FormToolbarActionProperty {

    public XlsActionProperty() {
        super("formXls", ApiResourceBundle.getString("form.layout.xls"), false, null,
              new CalcProperty[] {FormEntity.isFullClient, FormEntity.isDialog}, new boolean[] {false, true});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.delayUserInterfaction(new RunOpenInExcelClientAction());
    }
}
