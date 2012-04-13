package retail.api.remote;

import retail.api.remote.MachineryInfo;
import retail.api.remote.TransactionInfo;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.List;

public interface MachineryHandler<T extends TransactionInfo, M extends MachineryInfo> extends Serializable {

    public abstract void sendTransaction(T transactionInfo, List<M> machineryInfoList) throws FileNotFoundException, UnsupportedEncodingException;
}
