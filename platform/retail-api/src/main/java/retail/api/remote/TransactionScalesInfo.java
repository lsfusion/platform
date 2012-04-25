package retail.api.remote;

import java.io.IOException;
import java.util.List;

public class TransactionScalesInfo extends TransactionInfo<ScalesInfo> {

    public TransactionScalesInfo(String groupID, Integer id, String dateTimeCode, List<ItemInfo> itemsList,
                                 List<ScalesInfo> machineryInfoList) {
        this.groupID = groupID;
        this.id = id;
        this.dateTimeCode = dateTimeCode;
        this.itemsList = itemsList;
        this.machineryInfoList = machineryInfoList;
    }

    @Override
    public void sendTransaction(Object handler, List<ScalesInfo> machineryInfoList) throws IOException {
        ((ScalesHandler)handler).sendTransaction(this, machineryInfoList);
    }
}
