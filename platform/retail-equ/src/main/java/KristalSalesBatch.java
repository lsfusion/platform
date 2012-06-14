import retail.api.remote.MachineryInfo;
import retail.api.remote.SalesBatch;
import retail.api.remote.SalesInfo;

import java.util.List;

public class KristalSalesBatch extends SalesBatch {
    public List<String> readFiles;

    public KristalSalesBatch(List<SalesInfo> salesInfoList, List<String> readFiles) {
        this.salesInfoList = salesInfoList;
        this.readFiles = readFiles;
    }
}
