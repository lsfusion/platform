package platform.server.classes;

import platform.interop.Data;

import java.text.DateFormat;
import java.text.Format;
import java.util.Calendar;

public class YearClass extends IntegerClass {

    public final static YearClass instance = new YearClass();
    private final static String sid = "YearClass";

    public YearClass() {
        DataClass.storeClass(sid, instance);
    }

    @Override
    public Format getDefaultFormat() {
        return DateFormat.getDateInstance(DateFormat.SHORT);
    }

    @Override
    public String toString() {
        return "Год";
    }

    @Override
    public Object getDefaultValue() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    @Override
    public byte getTypeID() {
        return Data.YEAR;
    }

    public String getSID() {
        return sid;
    }
}
