package roman.actions;

import java.io.Serializable;
import java.util.Date;

public class InvoiceProperties implements Serializable{
    public Date date;
    public Double quantityDocument;
    public Double sumDocument;
    public Double netWeightDocument;

    public InvoiceProperties(Date date, Double quantityDocument, Double sumDocument, Double netWeightDocument) {
        this.date = date;
        this.quantityDocument = quantityDocument;
        this.sumDocument = sumDocument;
        this.netWeightDocument = netWeightDocument;
    }
}
