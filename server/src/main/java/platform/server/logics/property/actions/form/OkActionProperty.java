package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.server.form.entity.FormEntity;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class OkActionProperty extends FormToolbarActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[] {FormEntity.isModal}, new boolean[] {false});

    public OkActionProperty() {
        super("formOk", ApiResourceBundle.getString("form.layout.ok"));
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formOk();
    }

    @Override
    protected LCP getPropertyCaption() {
        return showIf;
    }
}
