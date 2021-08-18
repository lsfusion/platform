package lsfusion.gwt.client.base;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.shared.DateTimeFormatInfo;
import lsfusion.gwt.client.base.jsni.HasNativeSID;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.jsni.NativeStringMap;
import lsfusion.gwt.client.view.MainFrame;

import java.util.*;
import java.util.stream.Stream;

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

    public static boolean isRedundantString(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static <K> String toString(String separator, K... array) {
        String result = "";
        for (K element : array)
            result = (result.length() == 0 ? "" : result + separator) + element;
        return result;
    }

    public static String replicate(char character, int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, character);
        return new String(chars);
    }

    public static String multiplyString(String string, int multiplier) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < multiplier; i++) {
            sb.append(string);
        }
        return sb.toString();
    }

    public static int countMatches(String str, String sub) {
        int count = 0;
        if (str != null && !str.isEmpty() && sub != null && !sub.isEmpty()) {
            for(int idx = 0; (idx = str.indexOf(sub, idx)) != -1; idx += sub.length()) {
                ++count;
            }
        }
        return count;
    }

    public static DateTimeFormat getDateFormat(String pattern, boolean edit) {
        return pattern != null ? DateTimeFormat.getFormat(pattern) : getDefaultDateFormat(edit);
    }

    public static DateTimeFormat getTimeFormat(String pattern) {
        return pattern != null ? DateTimeFormat.getFormat(pattern) : getDefaultTimeFormat();
    }

    public static DateTimeFormat getDateTimeFormat(String pattern, boolean edit) {
        return pattern != null ? DateTimeFormat.getFormat(pattern) : getDefaultDateTimeFormat(edit);
    }

    public static DateTimeFormat getDefaultDateFormat(boolean edit) {
        if(edit) {
            String pattern = getValidEditDateFormat(MainFrame.dateFormat, false);
            if(pattern != null) {
                return DateTimeFormat.getFormat(pattern);
            } else {
                return getDateFormatShort(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_SHORT));
            }
        } else {
            return DateTimeFormat.getFormat(MainFrame.dateFormat);
        }
    }

    public static DateTimeFormat getDefaultTimeFormat() {
        return DateTimeFormat.getFormat(MainFrame.timeFormat);
        //return DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.TIME_MEDIUM);
    }

    public static DateTimeFormat getDefaultTimeShortFormat() {
        return DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.TIME_SHORT);
    }

    public static DateTimeFormat getDefaultDateTimeFormat(boolean edit) {
        if(edit) {
            String pattern = getValidEditDateFormat(MainFrame.dateFormat + " " + MainFrame.timeFormat, true);
            if(pattern != null) {
                return DateTimeFormat.getFormat(pattern);
            } else {
                DateTimeFormatInfo info = LocaleInfo.getCurrentLocale().getDateTimeFormatInfo();
                return DateTimeFormat.getFormat(info.dateTime(info.timeFormatMedium(), getDateFormatShort(info.dateFormatShort())).replace(",", ""));
            }
        } else {
            return DateTimeFormat.getFormat(MainFrame.dateFormat + " " + MainFrame.timeFormat);
        }
    }

    //equal to BaseUtils.getValidEditDateFormat
    public static String getValidEditDateFormat(String pattern, boolean dateTime) {
        String regexp = dateTime ? "[^dMyHmsS\\s\\\\/.,\\-:]|M{3,}" : "[^dMy\\s\\\\/.,\\-:]|M{3,}";
        Stream<String> requiredSymbols = dateTime ? Stream.of("d", "M", "y", "H", "m") : Stream.of("d", "M", "y");
        pattern = pattern.replaceAll(regexp, "").trim();
        return requiredSymbols.allMatch(pattern::contains) ? pattern : null;
    }

    public static DateTimeFormat getDefaultDateTimeShortFormat() {
        DateTimeFormatInfo info = LocaleInfo.getCurrentLocale().getDateTimeFormatInfo();
        return DateTimeFormat.getFormat(info.dateTime(info.timeFormatShort(), getDateFormatShort(info.dateFormatShort())).replace(",", ""));
    }

    //in gwt 2.9.0 default DATE_SHORT format for "ru" locale is dd.MM.y
    private static String getDateFormatShort(String defaultFormat) {
        String currentLocale = LocaleInfo.getCurrentLocale().getLocaleName();
        if(currentLocale.equals("ru")) {
            return "dd.MM.yy";
        } else {
            return defaultFormat;
        }
    }
    private static DateTimeFormat getDateFormatShort(DateTimeFormat defaultFormat) {
        String currentLocale = LocaleInfo.getCurrentLocale().getLocaleName();
        if(currentLocale.equals("ru")) {
            return DateTimeFormat.getFormat("dd.MM.yy");
        } else {
            return defaultFormat;
        }
    }

    public static <B, K1 extends B, K2 extends B, V> NativeHashMap<B, V> override(NativeHashMap<K1, ? extends V> map1, NativeHashMap<K2, ? extends V> map2) {
        NativeHashMap<B, V> result = new NativeHashMap<B, V>();
        result.putAll(map1);
        result.putAll(map2);
        return result;
    }

    public static String nullEmpty(String string) {
        if (string != null && string.trim().isEmpty()) {
            return null;
        } else {
            return string;
        }
    }

    public static boolean nullEquals(Object obj1, Object obj2) {
        if (obj1 == null)
            return obj2 == null;
        else
            return obj1.equals(obj2);
    }

    public static Object nullBoolean(Boolean b) {
        if (b) return true;
        else return null;
    }

    public static String nullTrim(String string) {
        if (string == null)
            return "";
        else
            return string.trim();
    }

    public static String nullTrim(Object value) {
        if (value == null)
            return "";
        else
            return value.toString().trim();
    }

    public static <MK, K, V> void putUpdate(NativeHashMap<MK, NativeHashMap<K, V>> keyValues, MK key, NativeHashMap<K, V> values, boolean update) {
        if (update) {
            keyValues.put(key, GwtSharedUtils.<K, K, K ,V>override(keyValues.get(key), values));
        } else {
            keyValues.put(key, values);
        }
    }

    public static <MK extends HasNativeSID, K, V> void putUpdate(NativeSIDMap<MK, NativeHashMap<K, V>> keyValues, MK key, NativeHashMap<K, V> values, boolean update) {
        if (update) {
            keyValues.put(key, GwtSharedUtils.<K, K, K ,V>override(keyValues.get(key), values));
        } else {
            keyValues.put(key, values);
        }
    }

    public static <T> boolean containsAny(Collection<T> collection, Collection<T> contained) {
        for (T obj : contained) {
            if (collection.contains(obj)) {
                return true;
            }
        }

        return false;
    }

    public static <R, C, V> void putToDoubleMap(Map<R, HashMap<C, V>> doubleMap, R row, C column, V value) {
        HashMap<C, V> rowMap = doubleMap.get(row);
        if (rowMap == null) {
            doubleMap.put(row, rowMap = new HashMap<>());
        }
        rowMap.put(column, value);
    }

    public static <R, C, V> void putToDoubleNativeMap(NativeHashMap<R, NativeHashMap<C, V>> doubleMap, R row, C column, V value) {
        NativeHashMap<C, V> rowMap = doubleMap.get(row);
        if (rowMap == null) {
            doubleMap.put(row, rowMap = new NativeHashMap<>());
        }
        rowMap.put(column, value);
    }

    public static <R, C, V> V getFromDoubleMap(NativeHashMap<R, ? extends NativeHashMap<C, V>> doubleMap, R row, C column) {
        NativeHashMap<C, V> rowMap = doubleMap.get(row);
        return rowMap == null ? null : rowMap.get(column);
    }

    public static <R, C, V> V removeFromDoubleMap(NativeHashMap<R, ? extends NativeHashMap<C, V>> doubleMap, R row, C column) {
        V result = null;
        NativeHashMap<C, V> rowMap = doubleMap.get(row);
        if (rowMap != null) {
            result = rowMap.remove(column);
        }
        return result;
    }

    public static <R extends HasNativeSID, C, V> void putToDoubleNativeMap(NativeSIDMap<R, NativeHashMap<C, V>> doubleMap, R row, C column, V value) {
        NativeHashMap<C, V> rowMap = doubleMap.get(row);
        if (rowMap == null) {
            doubleMap.put(row, rowMap = new NativeHashMap<>());
        }
        rowMap.put(column, value);
    }

    public static <R extends HasNativeSID, C, V> V getFromDoubleMap(NativeSIDMap<R, ? extends NativeHashMap<C, V>> doubleMap, R row, C column) {
        NativeHashMap<C, V> rowMap = doubleMap.get(row);
        return rowMap == null ? null : rowMap.get(column);
    }

    public static <R extends HasNativeSID, C, V> V removeFromDoubleMap(NativeSIDMap<R, ? extends NativeHashMap<C, V>> doubleMap, R row, C column) {
        V result = null;
        NativeHashMap<C, V> rowMap = doubleMap.get(row);
        if (rowMap != null) {
            result = rowMap.remove(column);
        }
        return result;
    }

    public static <C, V> void putToDoubleNativeMap(NativeStringMap<NativeHashMap<C, V>> doubleMap, String row, C column, V value) {
        NativeHashMap<C, V> rowMap = doubleMap.get(row);
        if (rowMap == null) {
            doubleMap.put(row, rowMap = new NativeHashMap<>());
        }
        rowMap.put(column, value);
    }

    public static <C, V> V getFromDoubleMap(NativeStringMap<? extends NativeHashMap<C, V>> doubleMap, String row, C column) {
        NativeHashMap<C, V> rowMap = doubleMap.get(row);
        return rowMap == null ? null : rowMap.get(column);
    }

    public static <C, V> V removeFromDoubleMap(NativeStringMap<? extends NativeHashMap<C, V>> doubleMap, String row, C column) {
        V result = null;
        NativeHashMap<C, V> rowMap = doubleMap.get(row);
        if (rowMap != null) {
            result = rowMap.remove(column);
        }
        return result;
    }

    public static boolean hashEquals(Object obj1, Object obj2) {
        return obj1 == obj2 || (obj1.hashCode() == obj2.hashCode() && obj1.equals(obj2));
    }

    public static abstract class Group<G, K> {
        public abstract G group(K key);
    }

    public static <G, K> Map<G, Collection<K>> group(Group<G, K> getter, Iterable<K> keys) {
        Map<G, Collection<K>> result = new HashMap<>();
        for (K key : keys) {
            G group = getter.group(key);
            if (group != null) {
                Collection<K> groupList = result.get(group);
                if (groupList == null) {
                    groupList = new ArrayList<>();
                    result.put(group, groupList);
                }
                groupList.add(key);
            }
        }
        return result;
    }

    public static <G, K> Map<G, ArrayList<K>> groupList(Group<G, K> getter, ArrayList<K> keys) {
        Map<G, ArrayList<K>> result = new HashMap<>();
        for (K key : keys) {
            G group = getter.group(key);
            if(group!=null) {
                ArrayList<K> groupList = result.get(group);
                if (groupList == null) {
                    groupList = new ArrayList<>();
                    result.put(group, groupList);
                }
                groupList.add(key);
            }
        }
        return result;
    }

    private static final char[] randomsymbols = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private final static Random random = new Random();
    public static String randomString(int len) {

        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(randomsymbols[random.nextInt(randomsymbols.length)]);
        }
        return sb.toString();
    }

    public static <T extends JavaScriptObject> JsArray<T> toArray(Collection<T> collection) {
        JsArray<T> array = JavaScriptObject.createArray().cast();
        for(T element : collection)
            array.push(element);
        return array;
    }

    public static String stringFormat(final String format, final String... args) {
        String[] split = format.split("%s");
        final StringBuilder msg = new StringBuilder();
        for (int pos = 0; pos < split.length - 1; pos++) {
            msg.append(split[pos]);
            msg.append(args[pos]);
        }
        msg.append(split[split.length - 1]);
        if (args.length == split.length) {
            msg.append(args[args.length - 1]);
        }
        return msg.toString();
    }

    public static String getDownloadURL(String name, String displayName, String extension, boolean actionFile) {
        return  "downloadFile" + (actionFile ? "" : "/static") +
                "?name=" + name + (displayName != null ? "&displayName=" + displayName : "") + (extension != null ? "&extension=" + extension : "");
    }
}
