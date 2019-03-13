package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class ExecuteLocalEventsActionProperty extends ScriptingActionProperty {

    public ExecuteLocalEventsActionProperty(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String formId = (String) context.getSingleKeyObject();
        FormEntity formEntity = null;
        if(formId != null)
            formEntity = context.getBL().findForm(formId);
        DataSession session = context.getSession();
        if(formEntity != null) {
            session.pushSessionEventActiveForm(formEntity);
        }
        try {
            context.executeSessionEvents();
        } finally {
            if(formEntity != null)
                session.popSessionEventActiveForm();
        }
    }
}
