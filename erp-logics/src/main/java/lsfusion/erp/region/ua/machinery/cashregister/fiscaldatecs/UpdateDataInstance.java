package lsfusion.erp.region.ua.machinery.cashregister.fiscaldatecs;

import java.io.Serializable;
import java.util.List;

public class UpdateDataInstance implements Serializable {

    public List<UpdateDataOperator> operatorList;
    public List<UpdateDataTaxRate> taxRateList;

    public UpdateDataInstance(List<UpdateDataOperator> operatorList, List<UpdateDataTaxRate> taxRateList) {
        this.operatorList = operatorList;
        this.taxRateList = taxRateList;
    }
}
