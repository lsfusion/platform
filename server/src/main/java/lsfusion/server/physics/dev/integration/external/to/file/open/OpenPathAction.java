package lsfusion.server.physics.dev.integration.external.to.file.open;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.OpenFileClientAction;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.commons.io.FilenameUtils;

import java.util.Iterator;

public class OpenPathAction extends InternalAction {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface nameInterface;

    public OpenPathAction(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        sourceInterface = i.next();
        nameInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            String source = (String) context.getKeyValue(sourceInterface).getValue();
            String name = (String) context.getKeyValue(nameInterface).getValue();

            if (source != null) {
                context.delayUserInteraction(new OpenFileClientAction(new RawFileData(source),
                        name != null ? name : FilenameUtils.getBaseName(source), BaseUtils.getFileExtension(source)));
            }

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}