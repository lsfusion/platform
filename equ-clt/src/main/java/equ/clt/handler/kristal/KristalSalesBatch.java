package equ.clt.handler.kristal;

import equ.api.SalesBatch;
import equ.api.SalesInfo;

import java.util.List;

public class KristalSalesBatch extends SalesBatch {
    public List<String> readFiles;

    public KristalSalesBatch(List<SalesInfo> salesInfoList, List<String> readFiles) {
        this.salesInfoList = salesInfoList;
        this.readFiles = readFiles;
    }
}
