package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;

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
}
