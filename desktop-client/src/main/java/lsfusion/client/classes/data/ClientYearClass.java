package lsfusion.client.classes.data;

import lsfusion.interop.classes.DataType;

import java.text.NumberFormat;

public class ClientYearClass extends ClientIntegerClass {

    public final static ClientYearClass instance = new ClientYearClass();

    @Override
    public byte getTypeId() {
        return DataType.YEAR;
    }

    @Override
    public NumberFormat getDefaultFormat() {
        NumberFormat format = super.getDefaultFormat();
        format.setGroupingUsed(false);
        return format;
    }
}
