package equ.api;

import java.io.IOException;
import java.util.List;

public class TransactionScalesInfo extends TransactionInfo<ScalesInfo> {
    public boolean snapshot;
    public TransactionScalesInfo(Integer id, String dateTimeCode, List<ItemInfo> itemsList,
                                 List<ScalesInfo> machineryInfoList, boolean snapshot) {
        this.id = id;
        this.dateTimeCode = dateTimeCode;
        this.itemsList = itemsList;
        this.machineryInfoList = machineryInfoList;
        this.snapshot = snapshot;
    }

    @Override
    public void sendTransaction(Object handler, List<ScalesInfo> machineryInfoList) throws IOException {
        ((ScalesHandler)handler).sendTransaction(this, machineryInfoList);
    }
}
