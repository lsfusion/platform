package retail.api.remote;

public interface TerminalHandler<S extends SalesBatch> extends MachineryHandler<TransactionTerminalInfo, TerminalInfo, S> {
}
