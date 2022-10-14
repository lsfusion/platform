package lsfusion.server.logics.classes.data.utils;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

public class CreateAutoReportAction extends InternalAction {
    private final ClassPropertyInterface formSIDInterface;

    public CreateAutoReportAction(SystemEventsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        formSIDInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String formSID = (String) context.getDataKeyValue(formSIDInterface).getValue();
        try {
            List<String> reportPathList = FormInstance.saveAndGetCustomReportPathList(context.getBL().findForm(formSID), false);
            String autoReportPath = String.join(";", reportPathList);
            findProperty("evalServerResult[]").change(autoReportPath, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}