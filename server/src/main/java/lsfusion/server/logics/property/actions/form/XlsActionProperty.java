package lsfusion.server.logics.property.actions.form;

import lsfusion.base.ApiResourceBundle;
import lsfusion.interop.action.RunOpenInExcelClientAction;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class XlsActionProperty extends FormToolbarActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[] {FormEntity.isFullClient, FormEntity.isDialog}, new boolean[] {false, true});

    public XlsActionProperty() {
        super("formXls", ApiResourceBundle.getString("form.layout.xls"), false);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.delayUserInterfaction(new RunOpenInExcelClientAction());
    }

    @Override
    protected LCP getShowIf() {
        return showIf;
    }
}
