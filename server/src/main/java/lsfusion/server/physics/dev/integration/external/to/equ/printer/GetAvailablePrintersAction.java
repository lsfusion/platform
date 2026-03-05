package lsfusion.server.physics.dev.integration.external.to.equ.printer;

import lsfusion.base.printer.GetAvailablePrintersClientAction;
import lsfusion.interop.action.MessageClientType;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

public class GetAvailablePrintersAction extends InternalAction {

    public GetAvailablePrintersAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String printerNames = (String) context.requestUserInteraction(new GetAvailablePrintersClientAction());
        boolean error = printerNames.isEmpty();
        context.message(error ? "No available printers found" : printerNames, "List of available printers", error ? MessageClientType.ERROR : MessageClientType.SUCCESS);
    }

}
