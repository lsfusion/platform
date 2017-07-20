package lsfusion.server.logics.property.actions.form;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class DropActionProperty extends FormFlowActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[] {FormEntity.showDrop}, new boolean[] {false});

    public DropActionProperty(BaseLogicsModule lm) {
        super(lm);
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        form.formDrop(context);
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
