package equ.clt.handler.maxishop;

import equ.api.SalesBatch;
import equ.api.SalesInfo;

import java.util.List;

public class MaxishopSalesBatch extends SalesBatch {
    public List<String> readFiles;
    
    public MaxishopSalesBatch(List<SalesInfo> salesInfoList, List<String> readFiles) {
        this.salesInfoList = salesInfoList;
        this.readFiles = readFiles;
    }
}
