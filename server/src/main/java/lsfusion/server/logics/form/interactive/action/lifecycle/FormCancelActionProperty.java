package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.session.DataSession;

import java.sql.SQLException;

public class FormCancelActionProperty extends FormFlowActionProperty {
    private static LCP showIf = createShowIfProperty(new Property[] {FormEntity.manageSession, FormEntity.isAdd}, new boolean[] {false, true});

    public FormCancelActionProperty(BaseLogicsModule lm) {
        super(lm);
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        form.formCancel(context);
    }

    @Override
    protected Property getEnableIf() {
        return DataSession.isDataChanged;
    }

    @Override
    protected LCP getShowIf() {
        return showIf;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if (type == ChangeFlowType.READONLYCHANGE)
            return true;
        return super.hasFlow(type);
    }
}
