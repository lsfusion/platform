package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.interop.action.RunPrintReportClientAction;
import platform.server.form.entity.FormEntity;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class PrintActionProperty extends FormToolbarActionProperty {

    public PrintActionProperty() {
        super("formPrint", ApiResourceBundle.getString("form.layout.print"), false, null,
              new CalcProperty[] {FormEntity.isFullClient, FormEntity.isDialog}, new boolean[] {false, true});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.delayUserInterfaction(new RunPrintReportClientAction());
    }
}
