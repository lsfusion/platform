package lsfusion.server.physics.dev.integration.external.to.equ;

import lsfusion.base.file.FileData;
import lsfusion.interop.action.BeepClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;

import java.sql.SQLException;
import java.util.Iterator;

public class BeepActionProperty extends ScriptingAction {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface asyncInterface;


    public BeepActionProperty(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        fileInterface = i.next();
        asyncInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
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