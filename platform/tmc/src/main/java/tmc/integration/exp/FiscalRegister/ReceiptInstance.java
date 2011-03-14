package tmc.integration.exp.FiscalRegister;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ReceiptInstance implements Serializable {
    int payType;
    public Double sumDisc;
    public Double sumCard;
    public Double sumCash;
    public Double sumTotal;
    public String cashierName;
    public String clientName;
    public Double clientSum;
    public Number clientDiscount; //скидка без учета сертификатов
    public List<ReceiptItem> receiptList;
    public List<ObligationItem> obligationList;

    public ReceiptInstance(int payType) {
        this.payType = payType;
        receiptList = new ArrayList<ReceiptItem>();
        obligationList = new ArrayList<ObligationItem>();
    }

    public void addReceipt(ReceiptItem item) {
        receiptList.add(item);
    }

    public void addObligation(ObligationItem item) {
        obligationList.add(item);
    }

}
