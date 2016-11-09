package lsfusion.server.logics.property.actions.form;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class OkActionProperty extends FormToolbarActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[] {FormEntity.isModal}, new boolean[] {false});

    public OkActionProperty(BaseLogicsModule lm) {
        super(lm);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.getFormInstance().formOk(context);
    }

    @Override
    protected LCP getShowIf() {
        return showIf;
    }

    @Override
    public boolean ignoreReadOnlyPolicy() {
        return true;
    }
}
