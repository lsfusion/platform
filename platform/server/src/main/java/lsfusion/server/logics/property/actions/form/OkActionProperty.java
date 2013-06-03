package lsfusion.server.logics.property.actions.form;

import lsfusion.base.ApiResourceBundle;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

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
