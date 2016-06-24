package lsfusion.erp.utils.utils;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.util.Iterator;


public class PrintToLogActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface stringInterface;

    public PrintToLogActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        stringInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        System.out.println((String) context.getDataKeyValue(stringInterface).object);
    }
}