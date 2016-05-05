package lsfusion.server.lib;

import com.google.common.base.Throwables;
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
    private final ClassPropertyInterface filenameInterface;

    public LoadDownloadedFontActionProperty(BaseLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        pathInterface = i.next();
        filenameInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject path = context.getDataKeyValue(pathInterface);
        DataObject filename = context.getDataKeyValue(filenameInterface);
        if (path != null && filename != null) {
            String result = (String) context.requestUserInteraction(new LoadDownloadedFontClientAction((String) path.getValue(), (String) filename.getValue()));
            if (result != null) {
                Throwables.propagate(new RuntimeException(result));
            }
        }
    }
}