package equ.clt.handler.shtrihPrint;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.jacob.com.LibraryLoader;
import com.jacob.com.Variant;
import equ.api.ItemInfo;
import equ.api.ScalesHandler;
import equ.api.ScalesInfo;
import equ.api.TransactionScalesInfo;

import java.io.*;
import java.util.*;

public class ShtrihPrintHandler extends ScalesHandler {

    public ShtrihPrintHandler() {
    }

    @Override
    public void sendTransaction(TransactionScalesInfo transactionInfo, List<ScalesInfo> machineryInfoList) throws IOException {

        System.setProperty(LibraryLoader.JACOB_DLL_PATH, "D:\\Projects\\platform\\equ-clt\\conf\\Shtrih\\jacob-1.15-M3.dll");

        ActiveXComponent shtrihActiveXComponent = new ActiveXComponent("AddIn.DrvLP");
        Dispatch shtrihDispatch = shtrihActiveXComponent.getObject();

        Variant pass = new Variant(30);

        Variant result = Dispatch.call(shtrihDispatch, "Connect");
        if (result.toString().equals("0")) {
            for (ItemInfo item : transactionInfo.itemsList) {
                Integer barcode = Integer.parseInt(item.idBarcode.substring(0, 5));
                int deltaDaysExpiry = item.expirationDate == null ? 0 : (int) ((item.expirationDate.getTime() - System.currentTimeMillis()) / 1000 / 3600 / 24);
                Integer shelfLife = item.daysExpiry == null ? (deltaDaysExpiry >= 0 ? deltaDaysExpiry : 0) : item.daysExpiry.intValue();

                int len = item.name.length();
                String firstName = item.name.substring(0, len < 28 ? len : 28);
                String secondName = len < 28 ? "" : item.name.substring(28, len < 56 ? len : 56);

                shtrihActiveXComponent.setProperty("Password", pass);
                shtrihActiveXComponent.setProperty("PLUNumber", new Variant(barcode));
                shtrihActiveXComponent.setProperty("Price", new Variant(item.price));
                shtrihActiveXComponent.setProperty("Tare", new Variant(0));
                shtrihActiveXComponent.setProperty("ItemCode", new Variant(barcode));
                shtrihActiveXComponent.setProperty("NameFirst", new Variant(firstName));
                shtrihActiveXComponent.setProperty("NameSecond", new Variant(secondName));
                shtrihActiveXComponent.setProperty("ShelfLife", new Variant(shelfLife)); //срок хранения в днях
                shtrihActiveXComponent.setProperty("GroupCode", new Variant(item.numberGroupItem));
                shtrihActiveXComponent.setProperty("PictureNumber", new Variant(0));
                shtrihActiveXComponent.setProperty("ROSTEST", new Variant(0));
                shtrihActiveXComponent.setProperty("ExpiryDate", new Variant(item.expirationDate));
                shtrihActiveXComponent.setProperty("GoodsType", new Variant(item.isWeightItem ? 0 : 1));

                for (int i = 0; i <= item.composition.length() / 50; i++) {
                    shtrihActiveXComponent.setProperty("MessageNumber", new Variant(item.compositionNumber));
                    shtrihActiveXComponent.setProperty("StringNumber", new Variant(i+1));
                    shtrihActiveXComponent.setProperty("MessageString", new Variant(item.composition.substring(50 * i, Math.min(50 * (i+1), item.composition.length()))));

                    result = Dispatch.call(shtrihDispatch, "SetMessageData");
                    if (!result.toString().equals("0")) {
                        throw new RuntimeException("ShtrihPrintHandler. Item # " + item.idBarcode + " Error # " + result.toString());
                    }
                }

                result = Dispatch.call(shtrihDispatch, "SetPLUData");
                if (!result.toString().equals("0")) {
                    throw new RuntimeException("ShtrihPrintHandler. Item # " + item.idBarcode + " Error # " + result.toString());
                }
            }
            result = Dispatch.call(shtrihDispatch, "Disconnect");
            if (!result.toString().equals("0")) {
                throw new RuntimeException("ShtrihPrintHandler. Disconnection error (# " + result.toString() + ")");
            }
        } else {
            throw new RuntimeException("ShtrihPrintHandler. Connection error (# " + result.toString() + ")");
        }
    }
}
