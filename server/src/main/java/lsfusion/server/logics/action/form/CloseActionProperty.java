package lsfusion.server.logics.action.form;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class CloseActionProperty extends FormFlowActionProperty {
    private static LCP showIf = createShowIfProperty(new CalcProperty[] {FormEntity.isSync}, new boolean[] {false});

    public CloseActionProperty(BaseLogicsModule lm) {
        super(lm);
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        form.formClose(context);
    }

    @Override
    protected LCP getShowIf() {
        return showIf;
    }
}
