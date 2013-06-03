package lsfusion.server.classes;

import lsfusion.interop.Data;
import lsfusion.server.logics.ServerResourceBundle;

import java.text.DateFormat;
import java.text.Format;
import java.util.Calendar;

public class YearClass extends IntegerClass {

    public final static YearClass instance = new YearClass();

    static {
        DataClass.storeClass(instance);
    }

    private YearClass() { caption = ServerResourceBundle.getString("classes.year"); }

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
        return "YearClass";
    }
}
