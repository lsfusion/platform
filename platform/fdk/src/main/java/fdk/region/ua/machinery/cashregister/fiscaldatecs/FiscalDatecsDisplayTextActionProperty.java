package fdk.region.ua.machinery.cashregister.fiscaldatecs;

import platform.interop.action.MessageClientAction;
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
import java.util.*;

public class FiscalDatecsDisplayTextActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface receiptDetailInterface;

    public FiscalDatecsDisplayTextActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.findClassByCompoundName("receiptDetail")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        receiptDetailInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {

        DataSession session = context.getSession();
        DataObject receiptDetailObject = context.getKeyValue(receiptDetailInterface);

        try {
            ObjectValue receiptObject = LM.findLCPByCompoundName("receiptReceiptDetail").readClasses(session, receiptDetailObject);
            Integer comPort = (Integer) LM.findLCPByCompoundName("comPortCurrentCashRegister").read(session);
            Integer baudRate = (Integer) LM.findLCPByCompoundName("baudRateCurrentCashRegister").read(session);

            String name = (String) LM.findLCPByCompoundName("nameSkuReceiptDetail").read(session, receiptDetailObject);
            String barcode = (String) LM.findLCPByCompoundName("idBarcodeReceiptDetail").read(session, receiptDetailObject);
            Double quantity = (Double) LM.findLCPByCompoundName("quantityReceiptDetail").read(session, receiptDetailObject);
            Double price = (Double) LM.findLCPByCompoundName("priceReceiptDetail").read(session, receiptDetailObject);
            Double sum = (Double) LM.findLCPByCompoundName("sumReceiptDetailReceipt").read(session, (DataObject)receiptObject);
            Double articleDisc = (Double) LM.findLCPByCompoundName("discountPercentReceiptSaleDetail").read(session, receiptDetailObject);
            Double articleDiscSum = (Double) LM.findLCPByCompoundName("discountSumReceiptDetail").read(session, receiptDetailObject);


            String result = (String)context.requestUserInteraction(new FiscalDatecsDisplayTextClientAction(baudRate, comPort, new ReceiptItem(price, quantity, barcode, name, sum, articleDisc, articleDiscSum, 0, 0)));
            if(result!=null)
                context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }
}
