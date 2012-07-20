package platform.gwt.utils;

import com.google.gwt.i18n.client.DateTimeFormat;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class GwtSharedUtils {
    public static <K> int relativePosition(K element, List<K> comparatorList, List<K> insertList) {
        int ins = 0;
        int ind = comparatorList.indexOf(element);

        Iterator<K> icp = insertList.iterator();
        while (icp.hasNext() && comparatorList.indexOf(icp.next()) < ind) {
            ins++;
        }
        return ins;
    }

    public static String rtrim(String string) {
        if (string == null) return "";

        int len = string.length();
        while (len > 0 && string.charAt(len - 1) == ' ') len--;
        return string.substring(0, len);
    }


    public static DateTimeFormat getDefaultDateFormat() {
        return DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT);
    }

    public static String formatDate(Date date) {
        return getDefaultDateFormat().format(date);
    }
}
