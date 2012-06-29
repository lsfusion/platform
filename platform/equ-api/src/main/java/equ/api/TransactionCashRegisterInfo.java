package equ.api;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class TransactionCashRegisterInfo extends TransactionInfo<CashRegisterInfo> {

    public TransactionCashRegisterInfo(Integer id, String dateTimeCode, Date date, List<ItemInfo> itemsList,
                                       List<CashRegisterInfo> machineryInfoList) {
        this.id = id;
        this.dateTimeCode = dateTimeCode;
        this.date = date;
        this.itemsList = itemsList;
        this.machineryInfoList = machineryInfoList;
    }

    @Override
    public void sendTransaction(Object handler, List<CashRegisterInfo> machineryInfoList) throws IOException {
        ((CashRegisterHandler)handler).sendTransaction(this, machineryInfoList);
    }
}
