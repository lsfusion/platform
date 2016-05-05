package lsfusion.server.lib;

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

public class LoadDownloadedLibraryActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface pathInterface;
    private final ClassPropertyInterface filenameInterface;

    public LoadDownloadedLibraryActionProperty(BaseLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        pathInterface = i.next();
        filenameInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject path = context.getDataKeyValue(pathInterface);
        DataObject filename = context.getDataKeyValue(filenameInterface);
        if (path != null && filename != null)
            context.requestUserInteraction(new LoadDownloadedLibraryClientAction((String) path.getValue(), (String) filename.getValue()));
    }
}