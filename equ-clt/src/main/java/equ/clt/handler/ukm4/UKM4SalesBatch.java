package equ.clt.handler.ukm4;

import equ.api.SalesBatch;
import equ.api.SalesInfo;

import java.util.List;

public class UKM4SalesBatch extends SalesBatch {
    public List<String> readFiles;

    public UKM4SalesBatch(List<SalesInfo> salesInfoList, List<String> readFiles) {
        this.salesInfoList = salesInfoList;
        this.readFiles = readFiles;
    }
}
