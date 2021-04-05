package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.file.client.MkdirClientAction;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.IOException;
import java.util.Iterator;

public class MkdirAction extends InternalAction {
    private final ClassPropertyInterface directoryInterface;
    private final ClassPropertyInterface isClientInterface;

    public MkdirAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        directoryInterface = i.next();
        isClientInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String directory = BaseUtils.trimToNull((String) context.getKeyValue(directoryInterface).getValue());
        boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;
        if (directory != null) {
            if (isClient) {
                String result = (String) context.requestUserInteraction(new MkdirClientAction(directory));
                if (result != null) {
                    throw new RuntimeException(result);
                }
            } else {
                FileUtils.mkdir(directory);
            }
        } else {
            throw new RuntimeException("Path not specified");
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}