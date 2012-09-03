package platform.gwt.utils;

import com.google.gwt.i18n.client.DateTimeFormat;

import java.util.*;

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

    public static <B, K1 extends B, K2 extends B, V> Map<B, V> override(Map<K1, ? extends V> map1, Map<K2, ? extends V> map2) {
        HashMap<B, V> result = new HashMap<B, V>(map1);
        result.putAll(map2);
        return result;
    }

    public static <MK, K, V> void putUpdate(Map<MK, Map<K, V>> keyValues, MK key, Map<K, V> values, boolean update) {
        if (update) {
            keyValues.put(key, override(keyValues.get(key), values));
        } else {
            keyValues.put(key, values);
        }
    }
}
