package retail.api.remote;

import retail.api.remote.CashRegisterInfo;
import retail.api.remote.MachineryHandler;
import retail.api.remote.TransactionCashRegisterInfo;

public interface CashRegisterHandler<S extends SalesBatch> extends MachineryHandler<TransactionCashRegisterInfo, CashRegisterInfo, S> {
}
