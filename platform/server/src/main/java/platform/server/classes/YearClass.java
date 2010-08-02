package platform.server.classes;

import java.text.DateFormat;
import java.text.Format;
import java.util.Calendar;

public class YearClass extends IntegerClass {

    public final static YearClass instance = new YearClass();

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
}
