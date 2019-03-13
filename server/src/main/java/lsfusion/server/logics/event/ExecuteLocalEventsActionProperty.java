package lsfusion.server.logics.event;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.logics.action.session.DataSession;

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
