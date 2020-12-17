package lsfusion.server.physics.dev.integration.external.to.equ;

import lsfusion.base.file.FileData;
import lsfusion.interop.action.BeepClientAction;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class BeepAction extends InternalAction {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface asyncInterface;


    public BeepAction(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        fileInterface = i.next();
        asyncInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        //do not use large files
        final FileData inputFile = (FileData) context.getKeyValue(fileInterface).getValue();
        boolean async = context.getKeyValue(asyncInterface).getValue() != null;
        if(inputFile != null) {
            context.delayUserInteraction(new BeepClientAction(inputFile.getRawFile(), async));
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}