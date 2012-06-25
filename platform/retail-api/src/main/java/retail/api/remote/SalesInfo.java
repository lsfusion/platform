package retail.api.remote;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;

public class SalesInfo implements Serializable {
    public String cashRegisterNumber;
    public String zReportNumber;
    public Integer billNumber;
    public Date date;
    public Time time;
    public Double sumBill;
    public Double sumCard;
    public Double sumCash;
    public String barcodeItem;
    public Double quantityBillDetail;
    public Double priceBillDetail;
    public Double sumBillDetail;
    public Double discountSumBillDetail;
    public Double discountSumBill;
    public Integer numberDiscountCard;
    public Integer numberBillDetail;
    public String filename;

    public SalesInfo(String cashRegisterNumber, String zReportNumber, Integer billNumber, Date date, Time time,
                     Double sumBill, Double sumCard, Double sumCash, String barcodeItem, Double quantityBillDetail,
                     Double priceBillDetail, Double sumBillDetail, Double discountSumBillDetail, Double discountSumBill,
                     Integer numberDiscountCard, Integer numberBillDetail, String filename) {
        this.cashRegisterNumber = cashRegisterNumber;
        this.zReportNumber = zReportNumber;
        this.billNumber = billNumber;
        this.date = date;
        this.time = time;
        this.sumBill = sumBill;
        this.sumCard = sumCard;
        this.sumCash = sumCash;
        this.barcodeItem = barcodeItem;
        this.quantityBillDetail = quantityBillDetail;
        this.priceBillDetail = priceBillDetail;
        this.sumBillDetail = sumBillDetail;
        this.discountSumBillDetail = discountSumBillDetail;
        this.discountSumBill = discountSumBill;
        this.numberDiscountCard = numberDiscountCard;
        this.numberBillDetail = numberBillDetail;
        this.filename = filename;
    }
}
