import retail.api.remote.MachineryInfo;
import retail.api.remote.SalesBatch;
import retail.api.remote.SalesInfo;

import java.util.List;

public class UKM4SalesBatch extends SalesBatch {

    public UKM4SalesBatch(List<SalesInfo> salesInfoList) {
        this.salesInfoList = salesInfoList;
    }
}
