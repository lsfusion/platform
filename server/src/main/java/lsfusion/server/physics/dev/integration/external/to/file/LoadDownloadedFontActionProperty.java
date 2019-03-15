package lsfusion.server.physics.dev.integration.external.to.file;

import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.physics.dev.integration.external.to.file.client.LoadDownloadedFontClientAction;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.Iterator;

public class LoadDownloadedFontActionProperty extends ScriptingAction {
    private final ClassPropertyInterface pathInterface;

    public LoadDownloadedFontActionProperty(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        pathInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject path = context.getDataKeyValue(pathInterface);
        if (path != null) {
            String result = (String) context.requestUserInteraction(new LoadDownloadedFontClientAction((String) path.getValue()));
            if (result != null) {
                ServerLoggers.systemLogger.error("LoadDownloadedFont Error: " + result);
            }
        }
    }
}