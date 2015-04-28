package lsfusion.erp.utils.printer;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.sql.SQLException;

public class GetAvailablePrintersActionProperty extends ScriptingActionProperty {

    public GetAvailablePrintersActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);

    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
        PrintRequestAttributeSet attributeSet = new HashPrintRequestAttributeSet();

        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(flavor, attributeSet);
        String printerNames = "";
        for (PrintService printService : printServices) {
            printerNames += printService.getName() + '\n';
        }

        context.requestUserInteraction(
                new MessageClientAction(printerNames.isEmpty() ? "Не найдено доступных принтеров" : printerNames, "Список доступных принтеров"));

    }

}
