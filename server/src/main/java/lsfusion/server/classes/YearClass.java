package lsfusion.server.classes;

import lsfusion.interop.form.property.DataType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.Calendar;

public class YearClass extends IntegerClass {

    public final static YearClass instance = new YearClass();

    static {
        DataClass.storeClass(instance);
    }

    private YearClass() { caption = LocalizedString.create("{classes.year}"); }

    @Override
    public Integer getDefaultValue() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    @Override
    public byte getTypeID() {
        return DataType.YEAR;
    }

    public String getSID() {
        return "YEAR";
    }
}
