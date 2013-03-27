package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.interop.action.RunPrintReportClientAction;
import platform.server.form.entity.FormEntity;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class PrintActionProperty extends FormToolbarActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[] {FormEntity.isFullClient, FormEntity.isDialog}, new boolean[] {false, true});

    public PrintActionProperty() {
        super("formPrint", ApiResourceBundle.getString("form.layout.print"), false);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.delayUserInterfaction(new RunPrintReportClientAction());
    }

    @Override
    protected LCP getPropertyCaption() {
        return showIf;
    }
}
