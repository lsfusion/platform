package lsfusion.server.physics.dev.integration.external.to.file.open;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.OpenFileClientAction;
import lsfusion.interop.action.OpenUriClientAction;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.link.LinkClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.io.FilenameUtils;

import java.net.URI;
import java.util.Iterator;

public class OpenAction extends InternalAction {
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
            boolean noWait = context.getKeyValue(noWaitInterface).getValue() != null;

            Type sourceType = sourceObject.getType();
            if (sourceType instanceof LinkClass) { // backward compatibility
                OpenLinkAction.execute(source, context, noWait, true);
            } else {
                String name = (String) context.getKeyValue(nameInterface).getValue();
                if (source != null) {
                    OpenFileClientAction action = new OpenFileClientAction(new RawFileData(source),
                            name != null ? name : FilenameUtils.getBaseName(source), BaseUtils.getFileExtension(source));
                    if (noWait) {
                        context.delayUserInteraction(action);
                    } else {
                        context.requestUserInteraction(action);
                    }
                }
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