package retail.api.remote;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;

public class SalesInfo implements Serializable {
    public String cashRegisterNumber;
    public Integer zReportNumber;
    public Integer billNumber;
    public Date date;
    public Time time;
    public Double sumBill;
    public String barcodeItem;
    public Double quantityBillDetail;
    public Double priceBillDetail;
    public Double sumBillDetail;
    public Double discountSumBillDetail;
    public Integer numberBillDetail;
    public String filename;

    public SalesInfo(String cashRegisterNumber, Integer zReportNumber, Integer billNumber, Date date, Time time,
                     Double sumBill, String barcodeItem, Double quantityBillDetail, Double priceBillDetail,
                     Double sumBillDetail, Double discountSumBillDetail, Integer numberBillDetail, String filename) {
        this.cashRegisterNumber = cashRegisterNumber;
        this.zReportNumber = zReportNumber;
        this.billNumber = billNumber;
        this.date = date;
        this.time = time;
        this.sumBill = sumBill;
        this.barcodeItem = barcodeItem;
        this.quantityBillDetail = quantityBillDetail;
        this.priceBillDetail = priceBillDetail;
        this.sumBillDetail = sumBillDetail;
        this.discountSumBillDetail = discountSumBillDetail;
        this.numberBillDetail = numberBillDetail;
        this.filename = filename;
    }
}
