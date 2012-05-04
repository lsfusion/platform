import retail.api.remote.SalesBatch;
import retail.api.remote.SalesInfo;

import java.util.List;

public class InventoryTechSalesBatch extends SalesBatch {
    public List<String> readFiles;

    public InventoryTechSalesBatch(List<SalesInfo> salesInfoList, List<String> readFiles) {
        this.salesInfoList = salesInfoList;
        this.readFiles = readFiles;
    }
}
