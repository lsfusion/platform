package lsfusion.server.physics.dev.integration.external.to.file.open;

import com.google.common.base.Throwables;
import lsfusion.interop.action.OpenUriClientAction;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.IOException;
import java.util.Iterator;

public class OpenLinkAction extends InternalAction {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface noWaitInterface;
    private final ClassPropertyInterface noEncodeInterface;

    public OpenLinkAction(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        sourceInterface = i.next();
        noWaitInterface = i.next();
        noEncodeInterface = i.next();
    }

    public static void execute(String source, ExecutionContext context, boolean noWait, boolean noEncode) throws IOException {
        for (String url : source.split(";")) {
            OpenUriClientAction action = new OpenUriClientAction(FormChanges.serializeConvertFileValue(url.trim(), context), noEncode); // trim is needed because space characters in str cause URISyntaxException
            if (noWait) {
                context.delayUserInteraction(action);
            } else {
                context.requestUserInteraction(action);
            }
        }
    }
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            String source = (String) context.getKeyValue(sourceInterface).getValue();
            boolean noWait = context.getKeyValue(noWaitInterface).getValue() != null;
            boolean noEncode = context.getKeyValue(noEncodeInterface).getValue() != null;
            if(source != null)
                execute(source, context, noWait, noEncode);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}