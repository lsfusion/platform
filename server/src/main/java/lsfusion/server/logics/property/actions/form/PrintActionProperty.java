package lsfusion.server.logics.property.actions.form;

import lsfusion.base.ApiResourceBundle;
import lsfusion.interop.action.RunPrintReportClientAction;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

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
    protected LCP getShowIf() {
        return showIf;
    }
}
