import retail.api.remote.MachineryInfo;
import retail.api.remote.SalesBatch;
import retail.api.remote.SalesInfo;

import java.util.List;

public class UKM4SalesBatch extends SalesBatch {
    public List<String> readFiles;

    public UKM4SalesBatch(List<SalesInfo> salesInfoList, List<String> readFiles) {
        this.salesInfoList = salesInfoList;
        this.readFiles = readFiles;
    }
}
