package lsfusion.server.physics.dev.integration.external.to.file;

import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.file.client.LoadDownloadedLibraryClientAction;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class LoadDownloadedLibraryAction extends InternalAction {
    private final ClassPropertyInterface pathInterface;

    public LoadDownloadedLibraryAction(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        pathInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        DataObject path = context.getDataKeyValue(pathInterface);
        if (path != null)
            context.requestUserInteraction(new LoadDownloadedLibraryClientAction((String) path.getValue()));
    }
}