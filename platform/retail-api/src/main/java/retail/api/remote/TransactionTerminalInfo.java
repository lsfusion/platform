package retail.api.remote;

import java.io.IOException;
import java.util.List;

public class TransactionTerminalInfo extends TransactionInfo<TerminalInfo> {

    public TransactionTerminalInfo(String groupID, Integer id, String dateTimeCode, List<ItemInfo> itemsList,
                                   List<TerminalInfo> machineryInfoList) {
        this.groupID = groupID;
        this.id = id;
        this.dateTimeCode = dateTimeCode;
        this.itemsList = itemsList;
        this.machineryInfoList = machineryInfoList;
    }

    @Override
    public void sendTransaction(Object handler, List<TerminalInfo> machineryInfoList) throws IOException {
        ((TerminalHandler)handler).sendTransaction(this, machineryInfoList);
    }
}
