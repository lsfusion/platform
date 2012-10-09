package roman.actions.fiscaldatecs;

import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;
import platform.interop.action.MessageClientAction;

import java.io.IOException;
import java.util.List;


public class FiscalDatecsPrintReceiptClientAction implements ClientAction {

    ReceiptInstance receipt;
    int baudRate;
    int comPort;
    int placeNumber;
    int operatorNumber;

    public FiscalDatecsPrintReceiptClientAction(Integer baudRate, Integer comPort, Integer placeNumber,
                                                Integer operatorNumber, ReceiptInstance receipt) {
        this.receipt = receipt;
        this.baudRate = baudRate == null ? 0 : baudRate;
        this.comPort = comPort == null ? 0 : comPort;
        this.placeNumber = placeNumber == null ? 1 : placeNumber;
        this.operatorNumber = operatorNumber == null ? 1 : operatorNumber;
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        if (receipt.receiptSaleList.size() != 0 && receipt.receiptReturnList.size() != 0)
            new MessageClientAction("В одном чеке обнаружены продажи и возврат одновременно", "Ошибка!");
        else {
            try {
                FiscalDatecs.init();

                FiscalDatecs.openPort(comPort, baudRate);

                int iCode = FiscalDatecs.getArticlesInfo() + 1;
                if(iCode==0)
                    return "Превышено максимально возможное количество артикулов";
                for (ReceiptItem item : receipt.receiptSaleList) {
                    FiscalDatecs.setArticle(iCode, item);
                    item.artNumber = iCode;
                    iCode++;
                }
                for (ReceiptItem item : receipt.receiptReturnList) {
                    FiscalDatecs.setArticle(iCode, item);
                    item.artNumber = iCode;
                    iCode++;
                }

                if (receipt.receiptSaleList.size() != 0)
                    if (!printReceipt(receipt.receiptSaleList, true))
                        return FiscalDatecs.getError();
                if (receipt.receiptReturnList.size() != 0)
                    if (!printReceipt(receipt.receiptReturnList, false))
                        return FiscalDatecs.getError();

                FiscalDatecs.closePort();

            } catch (RuntimeException e) {
                FiscalDatecs.cancelReceipt();
                return FiscalDatecs.getError();
            }
        }
        return null;
    }

    private boolean printReceipt(List<ReceiptItem> receiptList, boolean sale) {

        if(FiscalDatecs.getFiscalClosureStatus()==1)
            if(FiscalDatecs.cancelReceipt()!=0)
            return false;
        if(FiscalDatecs.openReceipt(operatorNumber, placeNumber, sale)!=0)
            return false;

        for (ReceiptItem item : receiptList) {
            if(FiscalDatecs.registerItem(item)!=0)
                return false;

        }
        if (FiscalDatecs.totalCard(receipt.sumCard, sale) != 0)
            return false;
        if (FiscalDatecs.totalCash(receipt.sumCash, sale) != 0)
            return false;
        return FiscalDatecs.closeReceipt() == 0;
    }
}
