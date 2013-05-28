package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.server.form.entity.FormEntity;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.session.DataSession;

import java.sql.SQLException;

public class FormCancelActionProperty extends FormToolbarActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[] {FormEntity.manageSession, FormEntity.isReadOnly}, new boolean[] {false, true});

    public FormCancelActionProperty() {
        super("formCancel", ApiResourceBundle.getString("form.layout.cancel"));
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formCancel();
    }

    @Override
    protected CalcProperty getEnableIf() {
        return DataSession.isDataChanged;
    }

    @Override
    protected LCP getPropertyCaption() {
        return showIf;
    }
}
