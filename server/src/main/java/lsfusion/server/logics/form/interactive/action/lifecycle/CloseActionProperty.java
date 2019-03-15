package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class CloseActionProperty extends FormFlowActionProperty {
    private static LP showIf = createShowIfProperty(new Property[] {FormEntity.isSync}, new boolean[] {false});

    public CloseActionProperty(BaseLogicsModule lm) {
        super(lm);
    }

    protected void executeForm(FormInstance form, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        form.formClose(context);
    }

    @Override
    protected LP getShowIf() {
        return showIf;
    }
}
