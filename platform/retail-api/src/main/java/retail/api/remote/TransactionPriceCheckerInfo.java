package retail.api.remote;

import java.io.IOException;
import java.util.List;

public class TransactionPriceCheckerInfo extends TransactionInfo<PriceCheckerInfo> {

    public TransactionPriceCheckerInfo(String groupID, Integer id, String dateTimeCode, List<ItemInfo> itemsList,
                                       List<PriceCheckerInfo> machineryInfoList) {
        this.groupID = groupID;
        this.id = id;
        this.dateTimeCode = dateTimeCode;
        this.itemsList = itemsList;
        this.machineryInfoList = machineryInfoList;
    }

    @Override
    public void sendTransaction(Object handler, List<PriceCheckerInfo> machineryInfoList) throws IOException {
        ((PriceCheckerHandler)handler).sendTransaction(this, machineryInfoList);
    }
}
