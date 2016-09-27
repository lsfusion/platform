package lsfusion.server.classes;

import lsfusion.interop.Data;
import lsfusion.server.logics.i18n.LocalizedString;

import java.util.Calendar;

public class YearClass extends IntegerClass {

    public final static YearClass instance = new YearClass();

    static {
        DataClass.storeClass(instance);
    }

    private YearClass() { caption = LocalizedString.create("{classes.year}"); }

    @Override
    public Object getDefaultValue() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    @Override
    public byte getTypeID() {
        return Data.YEAR;
    }

    public String getSID() {
        return "YEAR";
    }
}
