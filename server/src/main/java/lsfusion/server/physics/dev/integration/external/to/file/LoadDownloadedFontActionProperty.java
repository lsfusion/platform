package lsfusion.server.physics.dev.integration.external.to.file;

import lsfusion.server.ServerLoggers;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.physics.dev.integration.external.to.file.client.LoadDownloadedFontClientAction;

import java.sql.SQLException;
import java.util.Iterator;

public class LoadDownloadedFontActionProperty extends ScriptingActionProperty {
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