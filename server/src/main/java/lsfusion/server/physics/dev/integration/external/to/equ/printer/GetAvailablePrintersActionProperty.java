package lsfusion.server.physics.dev.integration.external.to.equ.printer;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.external.to.equ.printer.client.GetAvailablePrintersClientAction;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class GetAvailablePrintersActionProperty extends ScriptingAction {

    public GetAvailablePrintersActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String printerNames = (String) context.requestUserInteraction(new GetAvailablePrintersClientAction());
        context.requestUserInteraction(
                new MessageClientAction(printerNames.isEmpty() ? "Не найдено доступных принтеров" : printerNames, "Список доступных принтеров"));
    }

}
