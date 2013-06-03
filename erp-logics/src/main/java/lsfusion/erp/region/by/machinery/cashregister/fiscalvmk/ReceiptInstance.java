package lsfusion.erp.region.by.machinery.cashregister.fiscalvmk;

import java.io.Serializable;
import java.util.List;

public class ReceiptInstance implements Serializable {
    public Double sumDisc;
    public Double sumCard;
    public Double sumCash;
    public Double sumTotal;

    public List<ReceiptItem> receiptSaleList;
    public List<ReceiptItem> receiptReturnList;

    public ReceiptInstance(Double sumDisc, Double sumCard, Double sumCash, Double sumTotal,
                           List<ReceiptItem> receiptSaleList, List<ReceiptItem> receiptReturnList) {
        this.sumDisc = sumDisc;
        this.sumCard = sumCard;
        this.sumCash = sumCash;
        this.sumTotal = sumTotal;
        this.receiptSaleList = receiptSaleList;
        this.receiptReturnList = receiptReturnList;
    }
}
