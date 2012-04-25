package retail.api.remote;

import java.io.IOException;
import java.util.List;

public class TransactionCashRegisterInfo extends TransactionInfo<CashRegisterInfo> {

    public TransactionCashRegisterInfo(String groupID, Integer id, String dateTimeCode, List<ItemInfo> itemsList,
                                       List<CashRegisterInfo> machineryInfoList) {
        this.groupID = groupID;
        this.id = id;
        this.dateTimeCode = dateTimeCode;
        this.itemsList = itemsList;
        this.machineryInfoList = machineryInfoList;
    }

    @Override
    public void sendTransaction(Object handler, List<CashRegisterInfo> machineryInfoList) throws IOException {
        ((CashRegisterHandler)handler).sendTransaction(this, machineryInfoList);
    }
}
