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

    public FiscalDatecsPrintReceiptClientAction(Integer comPort, Integer baudRate, Integer placeNumber,
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

                int iCode = 1;
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
                    printReceipt(receipt.receiptSaleList, true);
                if (receipt.receiptReturnList.size() != 0)
                    printReceipt(receipt.receiptReturnList, false);

                FiscalDatecs.closePort();

                FiscalDatecs.closeWriter();

            } catch (RuntimeException e) {
                return FiscalDatecs.getError();
            }
        }
        return null;
    }

    private void printReceipt(List<ReceiptItem> receiptList, boolean sale) {

        FiscalDatecs.openReceipt(operatorNumber, placeNumber, sale);

        for (ReceiptItem item : receiptList) {
            FiscalDatecs.registerItem(item);
            if (item.articleDiscSum != null && item.articleDiscSum > 0) {
                String msg = "Скидка " + item.articleDisc + "%, всего " + item.articleDiscSum;
                FiscalDatecs.printFiscalText(msg);
            }
        }
        FiscalDatecs.totalCard(receipt.sumCard);
        FiscalDatecs.totalCash(receipt.sumCash);
        FiscalDatecs.closeReceipt(operatorNumber, placeNumber, sale);
        FiscalDatecs.closePort();
    }
}
