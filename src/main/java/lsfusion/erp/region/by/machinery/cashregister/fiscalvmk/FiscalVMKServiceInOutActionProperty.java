package lsfusion.erp.region.by.machinery.cashregister.fiscalvmk;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class FiscalVMKServiceInOutActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface cashOperationInterface;

    public FiscalVMKServiceInOutActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.findClassByCompoundName("CashOperation")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        cashOperationInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataObject cashOperationObject = context.getDataKeyValue(cashOperationInterface);

            Integer comPort = (Integer) LM.findLCPByCompoundName("comPortCurrentCashRegister").read(context.getSession());
            Integer baudRate = (Integer) LM.findLCPByCompoundName("baudRateCurrentCashRegister").read(context.getSession());
            Boolean isDone = LM.findLCPByCompoundName("isCompleteCashOperation").read(context.getSession(), cashOperationObject) != null;
            Double sum = (Double)LM.findLCPByCompoundName("sumCashOperation").read(context.getSession(), cashOperationObject);

            if (!isDone) {
                String result = (String) context.requestUserInteraction(new FiscalVMKServiceInOutClientAction(baudRate, comPort, sum));
                if (result == null){
                    LM.findLCPByCompoundName("isCompleteCashOperation").change(true, context.getSession(), cashOperationObject);
                }
                else
                    context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));
            }

        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
