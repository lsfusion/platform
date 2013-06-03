package fdk.region.ua.machinery.cashregister.fiscaldatecs;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class FiscalDatecsZReportActionProperty extends ScriptingActionProperty {

    public FiscalDatecsZReportActionProperty(ScriptingLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        try {
            DataSession session = context.getSession();

            Integer comPort = (Integer) LM.findLCPByCompoundName("comPortCurrentCashRegister").read(context.getSession());
            Integer baudRate = (Integer) LM.findLCPByCompoundName("baudRateCurrentCashRegister").read(context.getSession());

            if (context.checkApply()) {
                Object VATSumReceipt = context.requestUserInteraction(new FiscalDatecsCustomOperationClientAction(2, baudRate, comPort));
                if (VATSumReceipt instanceof Double[]) {
                    ObjectValue zReportObject = LM.findLCPByCompoundName("currentZReport").readClasses(session);
                    if (!zReportObject.isNull()) {
                        LM.findLCPByCompoundName("VATSumSaleZReport").change(((Object[]) VATSumReceipt)[0], session, (DataObject) zReportObject);
                        LM.findLCPByCompoundName("VATSumReturnZReport").change(((Object[]) VATSumReceipt)[1], session, (DataObject) zReportObject);
                    }
                    context.apply();
                    LM.findLAPByCompoundName("closeCurrentZReport").execute(session);
                } else if (VATSumReceipt != null)
                    context.requestUserInteraction(new MessageClientAction((String) VATSumReceipt, "Ошибка"));
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
