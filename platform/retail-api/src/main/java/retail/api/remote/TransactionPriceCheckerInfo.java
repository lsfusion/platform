package retail.api.remote;

import retail.api.remote.ItemInfo;
import retail.api.remote.PriceCheckerInfo;
import retail.api.remote.TransactionInfo;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
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
    public void sendTransaction(Object handler, List<PriceCheckerInfo> machineryInfoList) throws FileNotFoundException, UnsupportedEncodingException {
        ((PriceCheckerHandler)handler).sendTransaction(this, machineryInfoList);
    }
}
