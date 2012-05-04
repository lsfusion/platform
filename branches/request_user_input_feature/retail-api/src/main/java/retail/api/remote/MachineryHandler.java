package retail.api.remote;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.List;

public abstract class MachineryHandler<T extends TransactionInfo, M extends MachineryInfo, S extends SalesBatch>/* extends Serializable*/ {

    public RetailRemoteInterface remote;

    public abstract void sendTransaction(T transactionInfo, List<M> machineryInfoList) throws IOException;

    public void setRemoteObject(RetailRemoteInterface remote) {
        this.remote = remote;
    }
}
