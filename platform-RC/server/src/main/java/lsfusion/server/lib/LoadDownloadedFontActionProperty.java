package lsfusion.server.lib;

import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;
import java.util.Iterator;

public class LoadDownloadedFontActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface pathInterface;

    public LoadDownloadedFontActionProperty(BaseLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
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