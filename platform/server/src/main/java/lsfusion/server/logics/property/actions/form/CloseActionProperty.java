package lsfusion.server.logics.property.actions.form;

import lsfusion.base.ApiResourceBundle;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class CloseActionProperty extends FormToolbarActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[] {FormEntity.isModal}, new boolean[] {false});

    public CloseActionProperty() {
        super("formClose", ApiResourceBundle.getString("form.layout.close"));
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formClose();
    }

    @Override
    protected LCP getPropertyCaption() {
        return showIf;
    }
}
