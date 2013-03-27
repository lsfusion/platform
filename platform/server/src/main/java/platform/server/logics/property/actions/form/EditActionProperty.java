package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.interop.action.RunEditReportClientAction;
import platform.server.form.entity.FormEntity;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class EditActionProperty extends FormToolbarActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[] {FormEntity.isFullClient, FormEntity.isDebug, FormEntity.isDialog}, new boolean[] {false, false, true});

    public EditActionProperty() {
        super("formEdit", ApiResourceBundle.getString("form.layout.edit"), false);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.delayUserInterfaction(new RunEditReportClientAction());
    }

    @Override
    protected LCP getPropertyCaption() {
        return showIf;
    }
}
