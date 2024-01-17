package lsfusion.gwt.client.form.property;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.LocaleInfo;
import lsfusion.gwt.client.base.jsni.JSNIHelper;

public class IntegralPatternConverter {
    public static JavaScriptObject convert(String pattern) {
        JavaScriptObject options = JSNIHelper.createObject();
        DecimalPatternOptions pOptions = getDecimalPatternOptions(pattern);
        
        if (pOptions.isIntegerPattern) {
            JSNIHelper.setAttribute(options, "alias", "integer");
        } else {
            JSNIHelper.setAttribute(options, "alias", "decimal");
            if (pOptions.minFractionalLength == 0 || pOptions.minIntegerLength == pOptions.maxFractionalLength) { // .### or .000
                JSNIHelper.setAttribute(options, "digits", String.valueOf(pOptions.maxFractionalLength));
                if (pOptions.minFractionalLength == pOptions.maxFractionalLength) { // .000
                    JSNIHelper.setAttribute(options, "digitsOptional", false);
                }
            } else { // .00#
                JSNIHelper.setAttribute(options, "digits", pOptions.minFractionalLength + "," + pOptions.maxFractionalLength);
            }
        }
        
        if (pOptions.groupSize > 0) {
            JSNIHelper.setAttribute(options, "groupSeparator", LocaleInfo.getCurrentLocale().getNumberConstants().groupingSeparator());
        }
        JSNIHelper.setAttribute(options, "radixPoint", LocaleInfo.getCurrentLocale().getNumberConstants().decimalSeparator());
        return options;
    }
    
    private static class DecimalPatternOptions {
        public boolean isIntegerPattern;
        
        public int groupSize;
        public int minIntegerLength;
        
        public int minFractionalLength;
        public int maxFractionalLength;
        
        public DecimalPatternOptions(boolean isIP, int groupSize, int minIL, int minFL, int maxFL) {
            this.isIntegerPattern = isIP;
            this.groupSize = groupSize;
            this.minIntegerLength = minIL;
            this.minFractionalLength = minFL;
            this.maxFractionalLength = maxFL;
        }
    }
    
    private static DecimalPatternOptions getDecimalPatternOptions(String pattern) {
        int pointPos = pattern.indexOf('.');
        if (pointPos == -1) {
            return getIntegerPatternOptions(pattern);
        }
        
        DecimalPatternOptions options = getIntegerPatternOptions(pattern.substring(0, pointPos));
        String fractionPart = pattern.substring(pointPos + 1);
        options.isIntegerPattern = false;
        options.maxFractionalLength = fractionPart.length();
        for (int i = 0; i < fractionPart.length(); ++i) {
            if (fractionPart.charAt(i) != '0') break;
            ++options.minFractionalLength;
        }
        
        return options;
    }
    
    private static DecimalPatternOptions getIntegerPatternOptions(String pattern) {
        int groupSize = 0;
        int commaPos = pattern.lastIndexOf(',');
        if (commaPos != -1) {
            groupSize = pattern.length() - commaPos - 1;
        }
        
        int minLength = 0;
        for (int i = pattern.length() - 1; i >= 0; --i) {
            char ch = pattern.charAt(i);
            if (ch == ',') continue;
            if (ch != '0') break;
            ++minLength;
        }
        
        return new DecimalPatternOptions(true, groupSize, minLength, 0, 0);
    }
}
