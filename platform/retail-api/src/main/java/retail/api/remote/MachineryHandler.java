package retail.api.remote;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public interface MachineryHandler<T extends TransactionInfo, M extends MachineryInfo, S extends SalesBatch> extends Serializable {

    public abstract void sendTransaction(T transactionInfo, List<M> machineryInfoList) throws IOException;

    public abstract SalesBatch readSalesInfo(List<CashRegisterInfo> cashRegisterInfoList);
    
    public abstract void finishReadingSalesInfo(S salesBatch);
}
