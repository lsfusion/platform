package lsfusion.erp.utils;

import lsfusion.interop.action.BeepClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class BeepActionProperty extends ScriptingActionProperty {
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
        final byte[] inputFile = (byte[]) context.getKeyValue(fileInterface).getValue();
        boolean async = context.getKeyValue(asyncInterface).getValue() != null;
        if(inputFile != null) {
            context.delayUserInteraction(new BeepClientAction(inputFile, async));
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}