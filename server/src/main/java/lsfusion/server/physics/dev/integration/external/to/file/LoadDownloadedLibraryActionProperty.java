package lsfusion.server.physics.dev.integration.external.to.file;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.physics.dev.integration.external.to.file.client.LoadDownloadedLibraryClientAction;

import java.sql.SQLException;
import java.util.Iterator;

public class LoadDownloadedLibraryActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface pathInterface;

    public LoadDownloadedLibraryActionProperty(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        pathInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject path = context.getDataKeyValue(pathInterface);
        if (path != null)
            context.requestUserInteraction(new LoadDownloadedLibraryClientAction((String) path.getValue()));
    }
}