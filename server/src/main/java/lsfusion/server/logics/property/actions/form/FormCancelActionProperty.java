package lsfusion.server.logics.property.actions.form;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class FormCancelActionProperty extends FormToolbarActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[] {FormEntity.manageSession, FormEntity.isReadOnly}, new boolean[] {false, true});

    public FormCancelActionProperty(BaseLogicsModule lm) {
        super(lm);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getFormInstance().formCancel(context);
    }

    @Override
    protected CalcProperty getEnableIf() {
        return DataSession.isDataChanged;
    }

    @Override
    protected LCP getShowIf() {
        return showIf;
    }
}
