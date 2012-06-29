package equ.api;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public abstract class CashRegisterHandler<S extends SalesBatch> extends MachineryHandler<TransactionCashRegisterInfo, CashRegisterInfo, S> {

    public abstract SalesBatch readSalesInfo(List<CashRegisterInfo> cashRegisterInfoList) throws IOException, ParseException;

    public abstract void finishReadingSalesInfo(S salesBatch);

}
