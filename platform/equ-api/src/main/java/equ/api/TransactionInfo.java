package equ.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

public abstract class TransactionInfo <M extends MachineryInfo> implements Serializable {
    public Integer id;
    public String dateTimeCode;
    public Date date;
    public List<ItemInfo> itemsList;
    public List<M> machineryInfoList;

    public abstract void sendTransaction(Object handler, List<M> machineryInfoList) throws IOException;
}
