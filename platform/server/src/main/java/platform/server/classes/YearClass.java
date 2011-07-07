package platform.server.classes;

import platform.interop.Data;
import platform.server.logics.ServerResourceBundle;

import java.text.DateFormat;
import java.text.Format;
import java.util.Calendar;

public class YearClass extends IntegerClass {

    public final static YearClass instance = new YearClass();
    private final static String sid = "YearClass";
    static {
        DataClass.storeClass(sid, instance);
    }

    protected YearClass() { caption = ServerResourceBundle.getString("classes.year"); }

    @Override
    public Format getReportFormat() {
        return DateFormat.getDateInstance(DateFormat.SHORT);
    }

    @Override
    public String toString() {
        return ServerResourceBundle.getString("classes.year");
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
