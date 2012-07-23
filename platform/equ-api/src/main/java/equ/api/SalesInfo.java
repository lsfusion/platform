package equ.api;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;

public class SalesInfo implements Serializable {
    public String cashRegisterNumber;
    public String zReportNumber;
    public Integer receiptNumber;
    public Date date;
    public Time time;
    public Double sumReceipt;
    public Double sumCard;
    public Double sumCash;
    public String barcodeItem;
    public Double quantityReceiptDetail;
    public Double priceReceiptDetail;
    public Double sumReceiptDetail;
    public Double discountSumReceiptDetail;
    public Double discountSumReceipt;
    public String seriesNumberDiscountCard;
    public Integer numberReceiptDetail;
    public String filename;

    public SalesInfo(String cashRegisterNumber, String zReportNumber, Integer receiptNumber, Date date, Time time,
                     Double sumReceipt, Double sumCard, Double sumCash, String barcodeItem, Double quantityReceiptDetail,
                     Double priceReceiptDetail, Double sumReceiptDetail, Double discountSumReceiptDetail, Double discountSumReceipt,
                     String seriesNumberDiscountCard, Integer numberReceiptDetail, String filename) {
        this.cashRegisterNumber = cashRegisterNumber;
        this.zReportNumber = zReportNumber;
        this.receiptNumber = receiptNumber;
        this.date = date;
        this.time = time;
        this.sumReceipt = sumReceipt;
        this.sumCard = sumCard;
        this.sumCash = sumCash;
        this.barcodeItem = barcodeItem;
        this.quantityReceiptDetail = quantityReceiptDetail;
        this.priceReceiptDetail = priceReceiptDetail;
        this.sumReceiptDetail = sumReceiptDetail;
        this.discountSumReceiptDetail = discountSumReceiptDetail;
        this.discountSumReceipt = discountSumReceipt;
        this.seriesNumberDiscountCard = seriesNumberDiscountCard;
        this.numberReceiptDetail = numberReceiptDetail;
        this.filename = filename;
    }
}
