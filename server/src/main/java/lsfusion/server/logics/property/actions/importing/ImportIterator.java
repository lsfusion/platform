package lsfusion.server.logics.property.actions.importing;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.TimeClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassType;
import org.apache.commons.lang3.time.DateUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

public abstract class ImportIterator {
    protected abstract Object nextRow();
    protected abstract void release();

    protected DateFormat getDateFormat(ImOrderSet<LCP> properties, List<Integer> columns, Integer column) {
        ValueClass valueClass = properties.get(columns.indexOf(column)).property.getValueClass(ClassType.valuePolicy);
        DateFormat dateFormat = null;
        if (valueClass instanceof DateClass) {
            dateFormat = DateClass.getDateFormat();
        } else if (valueClass instanceof TimeClass) {
            dateFormat = ((TimeClass) valueClass).getDefaultFormat();
        } else if (valueClass instanceof DateTimeClass) {
            dateFormat = DateTimeClass.getDateTimeFormat();
        }
        return dateFormat;
    }

    protected String parseFormatDate(DateFormat dateFormat, String value) {
        String result = null;
        try {
            if (value != null && !value.isEmpty() && !value.replace(".", "").trim().isEmpty()) {
                Date date = DateUtils.parseDate(value, "dd/MM/yyyy", "dd.MM.yyyy", "dd.MM.yyyy HH:mm", "dd.MM.yyyy HH:mm:ss");
                if(date != null)
                    result = dateFormat.format(date);
            }
        } catch (ParseException ignored) {
        }
        return result;
    }
}