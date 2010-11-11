package tmc.integration.exp.FiscalRegistar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ReceiptInstance implements Serializable {
    int payType;
    public Double sumDisc;
    public Double sumCard;
    public Double sumCash;
    public List<ReceiptItem> list;

    public ReceiptInstance(int payType) {
        this.payType = payType;
        list = new ArrayList<ReceiptItem>();
    }
    
    public void add(ReceiptItem item){
        list.add(item);
    }

}
