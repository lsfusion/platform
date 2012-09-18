package roman.actions.fiscaldatecs;

import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

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

            if (context.checkApply(LM.getBL())) {
                Object VATSumReceipt = context.requestUserInteraction(new FiscalDatecsCustomOperationClientAction(2, baudRate, comPort));
                if (VATSumReceipt instanceof Double[]) {
                    ObjectValue receiptObject = LM.findLCPByCompoundName("currentZReport").readClasses(session);
                    if (!receiptObject.isNull()) {
                        LM.findLCPByCompoundName("VATSumSaleReceipt").change(((Object[]) VATSumReceipt)[0], session, (DataObject) receiptObject);
                        LM.findLCPByCompoundName("VATSumReturnReceipt").change(((Object[]) VATSumReceipt)[1], session, (DataObject) receiptObject);
                    }
                    LM.findLAPByCompoundName("fiscalPrintZReport").execute(session);
                    LM.findLAPByCompoundName("closeCurrentZReport").execute(session);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
