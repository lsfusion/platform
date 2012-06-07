import retail.api.remote.MachineryInfo;
import retail.api.remote.SalesBatch;
import retail.api.remote.SalesInfo;

import java.util.List;

public class KristalSalesBatch extends SalesBatch {

    public KristalSalesBatch(List<SalesInfo> salesInfoList) {
        this.salesInfoList = salesInfoList;
    }
}
