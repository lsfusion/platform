package fdk.region.by.machinery.cashregister.fiscalvmk;

import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.sql.SQLException;

public class FiscalVMKZReportActionProperty extends ScriptingActionProperty {

    public FiscalVMKZReportActionProperty(ScriptingLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataSession session = context.getSession();

            Integer comPort = (Integer) LM.findLCPByCompoundName("comPortCurrentCashRegister").read(context.getSession());
            Integer baudRate = (Integer) LM.findLCPByCompoundName("baudRateCurrentCashRegister").read(context.getSession());

            if (context.checkApply(LM.getBL())) {
               String result = (String)context.requestUserInteraction(new FiscalVMKCustomOperationClientAction(2, baudRate, comPort));
                if (result != null)
                    context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
