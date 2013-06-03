package fdk.region.by.machinery.cashregister.fiscalvmk;

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
import java.util.Iterator;

public class FiscalVMKDisplayTextActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface receiptDetailInterface;

    public FiscalVMKDisplayTextActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.findClassByCompoundName("ReceiptDetail")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        receiptDetailInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {

        DataSession session = context.getSession();
        DataObject receiptDetailObject = context.getDataKeyValue(receiptDetailInterface);

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


            String result = (String)context.requestUserInteraction(new FiscalVMKDisplayTextClientAction(baudRate, comPort, new ReceiptItem(price.longValue(), quantity, barcode, name, sum.longValue(), articleDisc, articleDiscSum, 0, 0)));
            if(result!=null)
                context.requestUserInteraction(new MessageClientAction(result, "Ошибка"));
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }
}
