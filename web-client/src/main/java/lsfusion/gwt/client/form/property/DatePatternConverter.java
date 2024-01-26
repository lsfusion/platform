package lsfusion.gwt.client.form.property;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.base.jsni.JSNIHelper;

import java.util.Arrays;

public class DatePatternConverter {
    
    public static JavaScriptObject convert(String pattern) {
        if(pattern == null)
            return null;

        JavaScriptObject options = JSNIHelper.createObject();
        JSNIHelper.setAttribute(options, "alias", "datetime");
        pattern = pattern + (char)0; // sentinel
        StringBuilder builder = new StringBuilder();
        char curCh = 0;
        int count = 1;
        for (int i = 0; i < pattern.length(); ++i) {
            char nextCh = pattern.charAt(i);
            if (nextCh == curCh) {
                ++count;
            } else {
                builder.append(convert(curCh, count));
                curCh = nextCh;
                count = 1;
            }
        }
//        com.allen_sauer.gwt.log.client.Log.error(builder.toString());
        JSNIHelper.setAttribute(options, "prefillYear", false);
        JSNIHelper.setAttribute(options, "inputFormat", builder.toString());
        return options;
    }
    
    private static String convert(char symbol, int count) {
        switch (symbol) {
            case 'd':
            case 'h':
            case 's':
                return repeat(symbol, Math.min(count, 2));
            case 'H':
                return "HH"; // there is no "H" in library
            case 'M':
                return repeat('m', Math.min(count, 4));
            case 'y':
                return count == 2 ? "yy" : "yyyy";
            case 'm':
                return count == 1 ? "M" : "MM";
            case 'E':
                return count < 4 ? "ddd" : "dddd";
            case 'a':
                return "TT";
        }
        return repeat(symbol, count);
    }
    
    private static String repeat(char ch, int count) {
        char[] buf = new char[count];
        Arrays.fill(buf, ch);
        return String.valueOf(buf);
    }
}
