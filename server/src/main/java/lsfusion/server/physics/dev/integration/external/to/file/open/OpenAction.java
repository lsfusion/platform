package lsfusion.server.physics.dev.integration.external.to.file.open;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.OpenFileClientAction;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.apache.commons.io.FilenameUtils;

import java.util.Iterator;

public class OpenAction extends AOpenAction {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface nameInterface;
    private final ClassPropertyInterface noWaitInterface;

    public OpenAction(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        sourceInterface = i.next();
        nameInterface = i.next();
        noWaitInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            ObjectValue sourceObject = context.getKeyValue(sourceInterface);
            String source = (String) sourceObject.getValue();
            String name = (String) context.getKeyValue(nameInterface).getValue();
            boolean noWait = context.getKeyValue(noWaitInterface).getValue() != null;

            if (source != null) {
                OpenFileClientAction action = new OpenFileClientAction(new RawFileData(source),
                        name != null ? name : FilenameUtils.getBaseName(source), BaseUtils.getFileExtension(source));
                if (noWait) {
                    context.delayUserInteraction(action);
                } else {
                    context.requestUserInteraction(action);
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}