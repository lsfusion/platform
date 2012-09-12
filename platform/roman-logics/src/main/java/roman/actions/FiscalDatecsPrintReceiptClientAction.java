package roman.actions;

import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;
import platform.interop.action.MessageClientAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
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

            //Dispatch cashDispatch = FiscalDatecs.getDispatch(comPort);
            PrintWriter zzz = new PrintWriter(new FileOutputStream(new File("D:/test/test_file.txt")));
            try {
                //Dispatch.call(cashDispatch, "ShowError", true);
                zzz.println("Dispatch.call(cashDispatch, \"ShowError\")" + " " + true);

                //Dispatch.call(cashDispatch, "OpenPort", comPort, baudRate);
                zzz.println("Dispatch.call(cashDispatch, \"OpenPort\")" + " " + comPort + " " + baudRate);

                int iCode = 1;
                String password = "0000";
                for (ReceiptItem item : receipt.receiptSaleList) {
                    //Dispatch.call(cashDispatch, "SetArticle", iCode, item.taxNumber, item.group, item.price, password, item.name);
                    zzz.println("Dispatch.call(cashDispatch, \"SetArticle\")" + " " + iCode + " " + item.taxNumber + " " + item.group + " " + item.price + " " + password + " " + item.name);
                    item.artNumber = iCode;
                    iCode++;
                }
                for (ReceiptItem item : receipt.receiptReturnList) {
                    //Dispatch.call(cashDispatch, "SetArticle", iCode, item.taxNumber, item.group, item.price, password, item.name);
                    zzz.println("Dispatch.call(cashDispatch, \"SetArticle\")" + " " + iCode + " " + item.taxNumber + " " + item.group + " " + item.price + " " + password + " " + item.name);
                    item.artNumber = iCode;
                    iCode++;
                }

                if (receipt.receiptSaleList.size() != 0)
                    printReceipt(zzz, password, receipt.receiptSaleList, true);
                if (receipt.receiptReturnList.size() != 0)
                    printReceipt(zzz, password, receipt.receiptReturnList, false);

                zzz.close();

            } catch (RuntimeException e) {
                //Dispatch.call(cashDispatch, "CancelReceipt");

                //Variant lastError =  cashDispatch.call(cashDispatch, "lastError");
                //Variant lastErrorText =  cashDispatch.call(cashDispatch, "lastErrorText");
                String lastError = "-1";
                String lastErrorText =  "some error";
                zzz.println("cashDispatch.call(cashDispatch, \"lastError)\")" + " " + lastError);
                zzz.println("cashDispatch.call(cashDispatch, \"lastErrorText)\")" + " " + lastErrorText);
                zzz.close();
                return lastError;
                //throw e;

            }
        }
        return null;
    }

    private void printReceipt(PrintWriter zzz, String password, List<ReceiptItem> receiptList, boolean sale) {
        //Dispatch.call(cashDispatch, sale? "OpenFiscalReceipt" : "OpenReturnReceipt", iOperator, password, iPlaceNumber);
        zzz.println("Dispatch.call(cashDispatch, " + (sale ? "OpenFiscalReceipt " : "OpenReturnReceipt ") + operatorNumber + " " + password + " " + placeNumber);

        for (ReceiptItem item : receiptList) {
            //Dispatch.call(cashDispatch, "RegistrItem", item.artNumber, item.quantity, 0, item.articleDiscSum);
            String itemMessage = "Dispatch.call(cashDispatch, \"RegistrItem\")" + " " + item.artNumber + " " + item.quantity + " " + 0 + " " + item.articleDiscSum;
            zzz.println(itemMessage);
            if (item.articleDiscSum != null && item.articleDiscSum > 0) {
                String msg = "Скидка " + item.articleDisc + "%, всего " + item.articleDiscSum;
                //Dispatch.call(cashDispatch, msg);
                zzz.println("Dispatch.call(cashDispatch)" + " " + msg);
            }
        }
        //Dispatch.call(cashDispatch, "Total", "Наличными: ", 1, receipt.sumCash);
        zzz.println("Dispatch.call(cashDispatch, \"Total\")" + " " + "Наличными: " + " " + 1 + " " + receipt.sumCash);
        //Dispatch.call(cashDispatch, "Total", "Карточкой: ", 4, receipt.sumCard);
        zzz.println("Dispatch.call(cashDispatch, \"Total\")" + " " + "Карточкой: " + " " + 4 + " " + receipt.sumCard);
        //Dispatch.call(cashDispatch, "CloseFiscalReceipt");
        zzz.println("Dispatch.call(cashDispatch, " + (sale ? "CloseFiscalReceipt " : "CloseReturnReceipt ") + operatorNumber + " " + password + " " + placeNumber);
        //Dispatch.call(cashDispatch, "ClosePort");
        zzz.println("Dispatch.call(cashDispatch, \"ClosePort\")");
    }
}
