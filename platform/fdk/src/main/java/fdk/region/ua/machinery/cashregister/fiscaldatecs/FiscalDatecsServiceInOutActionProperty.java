package fdk.region.ua.machinery.cashregister.fiscaldatecs;

import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.sql.SQLException;
import java.util.Iterator;

public class FiscalDatecsServiceInOutActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface cashOperationInterface;

    public FiscalDatecsServiceInOutActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.findClassByCompoundName("cashOperation")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        cashOperationInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataObject cashOperationObject = context.getKeyValue(cashOperationInterface);

            Integer comPort = (Integer) LM.findLCPByCompoundName("comPortCurrentCashRegister").read(context.getSession());
            Integer baudRate = (Integer) LM.findLCPByCompoundName("baudRateCurrentCashRegister").read(context.getSession());
            Boolean isDone = LM.findLCPByCompoundName("isCompleteCashOperation").read(context.getSession(), cashOperationObject) != null;
            Double sum = (Double)LM.findLCPByCompoundName("sumCashOperation").read(context.getSession(), cashOperationObject);

            if (!isDone) {
                String result = (String) context.requestUserInteraction(new FiscalDatecsServiceInOutClientAction(baudRate, comPort, sum));
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
