package equ.api;

import java.io.IOException;
import java.util.List;

public class TransactionTerminalInfo extends TransactionInfo<TerminalInfo> {
    public Boolean snapshot;
    
    public TransactionTerminalInfo(Integer id, String dateTimeCode, List<ItemInfo> itemsList, List<TerminalInfo> machineryInfoList,
                                   Boolean snapshot) {
        this.id = id;
        this.dateTimeCode = dateTimeCode;
        this.itemsList = itemsList;
        this.machineryInfoList = machineryInfoList;
        this.snapshot = snapshot;
    }

    @Override
    public void sendTransaction(Object handler, List<TerminalInfo> machineryInfoList) throws IOException {
        ((TerminalHandler)handler).sendTransaction(this, machineryInfoList);
    }
}
