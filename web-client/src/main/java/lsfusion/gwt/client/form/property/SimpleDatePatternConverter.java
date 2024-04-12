package lsfusion.gwt.client.form.property;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.JSNIHelper;

import java.util.Arrays;
import java.util.function.BiFunction;

public class SimpleDatePatternConverter {
    public static JavaScriptObject convert(String pattern, boolean interval) {
        JavaScriptObject options = JSNIHelper.createObject();
        String mask = patternToMask(pattern, SimpleDatePatternConverter::convertSymbol);
        if(interval)
            mask = GwtClientUtils.formatInterval(mask, mask);
//        com.allen_sauer.gwt.log.client.Log.error(mask);
        JSNIHelper.setAttribute(options, "insertMode", false);
        JSNIHelper.setAttribute(options, "insertModeVisual", false);
        JSNIHelper.setAttribute(options, "mask", mask);
        return options;
    }

    public static String patternToMask(String pattern, BiFunction<Character, Integer, String> convertSymbol) {
        pattern = pattern + (char)0; // sentinel
        StringBuilder builder = new StringBuilder();
        char curCh = 0;
        int count = 0;
        for (int i = 0; i < pattern.length(); ++i) {
            char nextCh = pattern.charAt(i);
            if (nextCh == curCh) {
                ++count;
            } else {
                builder.append(convertSymbol.apply(curCh, count));
                curCh = nextCh;
                count = 1;
            }
        }
        return builder.toString();
    }
    
    private static String convertSymbol(char symbol, int count) {
        switch (symbol) {
            case 'd':
            case 'h':
            case 's':
            case 'H':
            case 'm':
                return count < 2 ? "9{1,2}" : "99";
            case 'M':
                if (count > 3) return "a{1,20}";
                if (count == 3) return "aaa";
                return count < 2 ? "9{1,2}" : "99";
            case 'y':
                return count == 2 ? "99" : "9999";
            case 'a':
                return "AA";
        }
        return repeat(symbol, count);
    }
    
    public static String repeat(char ch, int count) {
        char[] buf = new char[count];
        Arrays.fill(buf, ch);
        return String.valueOf(buf);
    }
}
