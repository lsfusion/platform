import retail.api.remote.MachineryInfo;
import retail.api.remote.SalesBatch;
import retail.api.remote.SalesInfo;

import java.util.List;

public class MaxishopSalesBatch extends SalesBatch {
    public List<String> readFiles;
    
    public MaxishopSalesBatch(List<SalesInfo> salesInfoList, List<String> readFiles) {
        this.salesInfoList = salesInfoList;
        this.readFiles = readFiles;
    }
}
