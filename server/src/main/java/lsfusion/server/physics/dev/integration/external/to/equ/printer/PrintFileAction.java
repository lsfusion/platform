package lsfusion.server.physics.dev.integration.external.to.equ.printer;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.equ.printer.client.PrintFileClientAction;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class PrintFileAction extends InternalAction {
    private final ClassPropertyInterface fileInterface; //PDFFILE or STRING
    private final ClassPropertyInterface printerNameInterface;
    private final ClassPropertyInterface trayNameInterface;
    private final ClassPropertyInterface duplexInterface;

    public PrintFileAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        fileInterface = i.next();
        printerNameInterface = i.next();
        trayNameInterface = i.next();
        duplexInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            Object file = context.getKeyValue(fileInterface).getValue();
            if(file != null) {
                String printerName = (String) context.getKeyValue(printerNameInterface).getValue();
                String trayName = (String) context.getKeyValue(trayNameInterface).getValue();
                boolean duplex = context.getKeyValue(duplexInterface).getValue() != null;
                if(file instanceof RawFileData) {
                    context.delayUserInteraction(new PrintFileClientAction((RawFileData) file, null, printerName, trayName, duplex));
                } else {
                    assert file instanceof String;
                    context.delayUserInteraction(new PrintFileClientAction(null, (String) file, printerName, trayName, duplex));
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