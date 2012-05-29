package tmc.integration.exp.FiscalRegister;

import com.jacob.com.Dispatch;
import platform.interop.action.ClientAction;
import platform.interop.action.ExecuteClientAction;
import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;


public class CashRegPrintReceiptAction implements ClientAction {
    final static int FONT = 2;

    ReceiptInstance receipt;
    int type;
    int comPort;

    boolean dispose;

    public CashRegPrintReceiptAction(int type, int comPort, ReceiptInstance receipt) {
        this.receipt = receipt;
        this.type = type;
        this.comPort = comPort;

        String disposeProperty = System.getProperty("tmc.integration.exp.FiscalRegister.dispose");
        dispose = disposeProperty != null && disposeProperty.equals("true");
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        if (dispose) {
            FiscalReg.dispose("Before PrintReceipt");
        }

        Dispatch cashDispatch = FiscalReg.getDispatch(comPort);
        Dispatch.call(cashDispatch, "OpenFiscalDoc", type);

        try {
            //печать заголовка
            int k = FiscalReg.printHeaderAndNumbers(cashDispatch, receipt);

            //печать товаров
            for (ReceiptItem item : receipt.receiptList) {
                Dispatch.call(cashDispatch, "AddCustom", item.barCode, FONT, 0, k++);
                String name = item.name.substring(0, Math.min(item.name.length(), FiscalReg.WIDTH));
                Dispatch.call(cashDispatch, "AddCustom", name, FONT, 0, k++);

                Dispatch.invoke(cashDispatch, "AddItem", Dispatch.Method, new Object[]{0, item.price, false,
                        0, 1, 0, item.quantity * 1000, 3, 0, "шт.", 0, 0, k++, 0}, new int[1]);

                if (item.articleDiscSum.doubleValue() > 0) {
                    String msg = "Скидка " + item.articleDisc + "%, всего " + item.articleDiscSum;
                    Dispatch.call(cashDispatch, "AddCustom", msg, FONT, 0, k++);
                }
            }

            Dispatch.call(cashDispatch, "AddCustom", "Всего: " + receipt.sumTotal, FONT, 0, k++);
            if (receipt.clientDiscount != null) {
                Dispatch.call(cashDispatch, "AddCustom", "Скидка: " + receipt.clientDiscount, FONT, 0, k++);
            }

            //печать сертификатов
            if (!receipt.obligationList.isEmpty()) {
                Dispatch.call(cashDispatch, "AddCustom", FiscalReg.delimetr, FONT, 0, k++);
            }
            for (ObligationItem obligation : receipt.obligationList) {
                String name = obligation.name.substring(0, Math.min(obligation.name.length(), FiscalReg.WIDTH));
                String price = obligation.barcode + ": " + obligation.sum.intValue();
                String sum = price.substring(0, Math.min(price.length(), FiscalReg.WIDTH));
                Dispatch.call(cashDispatch, "AddCustom", name, FONT, 0, k++);
                Dispatch.call(cashDispatch, "AddCustom", sum, FONT, 0, k++);
            }

            //Общая информация
            Dispatch.call(cashDispatch, "AddCustom", FiscalReg.delimetr, FONT, 0, k++);
            if (receipt.sumDisc > 0) {
                Dispatch.call(cashDispatch, "AddDocAmountAdj", -receipt.sumDisc, 0, FONT, 0, k++, 15);
            }
            Dispatch.call(cashDispatch, "AddTotal", FONT, 0, k++, 15);

            if (type == 1) {
                //выбор варианта оплаты
                boolean needChange = true;
                if (receipt.sumCash > 0 && receipt.sumCard > 0) {
                    Dispatch.call(cashDispatch, "AddPay", 4, receipt.sumCash, receipt.sumCard, "Pay", FONT, 0, k++, 15);
                } else if (receipt.sumCard > 0) {
                    Dispatch.call(cashDispatch, "AddPay", 2, receipt.sumCard, receipt.sumCard, "Pay", FONT, 0, k++, 25);
                    needChange = false;
                } else {
                    Dispatch.call(cashDispatch, "AddPay", 0, receipt.sumCash, receipt.sumCard, "Pay", FONT, 0, k++, 15);
                }

                if (needChange) {
                    Dispatch.call(cashDispatch, "AddChange", FONT, 0, k++, 15);
                }
            }
            Dispatch.call(cashDispatch, "CloseFiscalDoc");

        } catch (RuntimeException e) {
            Dispatch.call(cashDispatch, "CancelFiscalDoc", false);
            throw e;
        }
        Dispatch.call(cashDispatch, "ExternalPulse", 1, 60, 10, 1);

        if (dispose) {
            FiscalReg.dispose("After PrintReceipt");
        }

        return true;
    }
}
