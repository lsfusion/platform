package lsfusion.base;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.ResourceBundle;

public class Numbers {

    public static ResourceBundle by = LocalizeUtils.getBundle("NumbersResourceBundle", Locale.forLanguageTag("by"));
    public static ResourceBundle ru = LocalizeUtils.getBundle("NumbersResourceBundle", Locale.forLanguageTag("ru"));
    public static ResourceBundle ua = LocalizeUtils.getBundle("NumbersResourceBundle", Locale.forLanguageTag("ua"));

    public final static int DG_POWER = 6;

    public static String toString(String lang, Object number) {
        return toString(lang, number, (Integer) null, false, false);
    }

    public static String toString(String lang, Object number, Integer numOfDigits) {
        return toString(lang, number, numOfDigits, false, false);
    }

    public static String toString(String lang, Object number, boolean female) {
        return toString(lang, number, (Integer) null, female, false);
    }

    //main method without type
    public static String toString(String lang, Object number, Integer numOfDigits, boolean female, boolean upcase) {
        String result;
        BigDecimal bigDecimal = getBigDecimal(number);
        int numOfDig = Math.min(numOfDigits != null ? numOfDigits : getMinFractLength(bigDecimal), 6);
        long fract = getFract(bigDecimal, numOfDig);
        if (fract != 0) {
            result = toString(lang, bigDecimal.longValue(), "number", 0, female, false, false) + " " + toString(lang, fract, "number", numOfDig, female, false, false);
        } else {
            result = toString(lang, bigDecimal.longValue(), "number", 0, female, false, true);
        }
        return upcase ? BaseUtils.capitalize(result) : result;
    }

    public static String toString(String lang, Object number, String type) {
        return toString(lang, number, type, false, null, false);
    }

    public static String toString(String lang, Object number, String type, boolean upcase) {
        return toString(lang, number, type, false, null, upcase);
    }

    public static String toString(String lang, Object number, String type, boolean numericFraction, boolean upcase) {
        return toString(lang, number, type, numericFraction, null, upcase);
    }

    //main method with type
    public static String toString(String lang, Object number, String type, boolean numericFraction, Integer forceNumOfDigits, boolean upcase) {
        String result;
        BigDecimal bigDecimal = getBigDecimal(number);
        Integer numOfDigits = getNumOfDigits(bigDecimal, type, forceNumOfDigits);
        long fract = getFract(bigDecimal, numOfDigits);
        if (fract != 0 || (forceNumOfDigits != null && forceNumOfDigits > 0)) {
            result = toString(lang, bigDecimal.longValue(), type, null, getSex(type), false, false) + " " + toString(lang, fract, type, numOfDigits, getSex(type + numOfDigits), numericFraction, false);
        } else {
            result = toString(lang, bigDecimal.longValue(), type, null, getSex(type), false, true);
        }

        return upcase ? BaseUtils.capitalize(result) : result;
    }

    private static String toString(String lang, Long value, String type, Integer numOfDigits, boolean female, boolean numericFraction, boolean noDecimalPostfix) {
        ResourceBundle bundle = getResourceBundle(lang);

        long sum = value == null ? 0 : value;

        int i, mny;
        StringBuilder result = new StringBuilder();
        long divisor; //делитель
        long psum = sum;

        int hun = 4;
        int dec = 3;
        int dec2 = 2;

        if (sum != 0) {
            if (sum < 0) {
                result.append(bundle.getString("minus"));
                psum = -psum;
            }

            for (i = 0, divisor = 1; i < DG_POWER; i++) divisor *= 1000;

            for (i = DG_POWER - 1; i >= 0; i--) {
                divisor /= 1000;
                mny = (int) (psum / divisor);
                psum %= divisor;
                //str="";
                if (mny == 0) {
                    if (i > 0) continue;
                    append(result, getPower(bundle, i, 0));
                } else {
                    if (mny >= 100) {
                        append(result, getDigit(bundle, mny / 100, hun));
                        mny %= 100;
                    }
                    if (mny >= 20) {
                        append(result, getDigit(bundle, mny / 10, dec));
                        mny %= 10;
                    }
                    if (mny >= 10) {
                        append(result, getDigit(bundle, mny - 10, dec2));
                    } else {
                        Integer sex = getSex(i, type, numOfDigits, female) ? 1 : 0;
                        if (mny >= 1) append(result, getDigit(bundle, mny, sex));
                    }

                    append(result, getPower(bundle, i, getVariation(mny)));
                }
            }
        } else append(result, bundle.getString("zero"));

        String postfix;
        if (numOfDigits != null && numOfDigits != 0) {
            postfix = getFractalPostfix(bundle, type, getVariation(value), numOfDigits);
        } else
            postfix = getDecimalPostfix(bundle, type, getVariation(value), noDecimalPostfix);

        return (numericFraction ? appendZeroes(value) : result.toString()) + (postfix.isEmpty() ? "" : " ") + postfix;
    }

    public static String toStringCustom(String lang, Object numObject, String decPostfix, String fractPostfix) {
        return toStringCustom(lang, numObject, decPostfix, fractPostfix, 2, null, false, false);
    }

    public static String toStringCustom(String lang, Object numObject, String decPostfix, String fractPostfix, boolean numericFraction, boolean upcase) {
        return toStringCustom(lang, numObject, decPostfix, fractPostfix, 2, null, numericFraction, upcase);
    }

    //main method with custom postfixes
    public static String toStringCustom(String lang, Object numObject, String decPostfix, String fractPostfix, int numOfFractDigits, Integer forceNumOfFractDigits, boolean numericFraction, boolean upcase) {
        BigDecimal num = getBigDecimal(numObject);
        long fract = getFract(num, numOfFractDigits);
        String result;
        if (fract != 0 || (forceNumOfFractDigits != null && forceNumOfFractDigits > 0))
            result = toStringCustom(lang, num.longValue(), decPostfix, fractPostfix, null, false, false) + " " + toStringCustom(lang, fract, decPostfix, fractPostfix, forceNumOfFractDigits != null ? Math.max(numOfFractDigits, forceNumOfFractDigits) : numOfFractDigits, true, numericFraction);
        else
            result = toStringCustom(lang, num.longValue(), decPostfix, fractPostfix, null, false, false);
        return upcase ? BaseUtils.capitalize(result) : result;
    }

    private static String toStringCustom(String lang, Long value, String decPostfix, String fractPostfix, Integer numOfDigits, boolean female, boolean numericFraction) {
        ResourceBundle bundle = getResourceBundle(lang);

        long sum = value == null ? 0 : value;

        int i, mny;
        StringBuilder result = new StringBuilder();
        long divisor; //делитель
        long psum = sum;

        int hun = 4;
        int dec = 3;
        int dec2 = 2;

        if (sum != 0) {
            if (sum < 0) {
                result.append(bundle.getString("minus"));
                psum = -psum;
            }

            for (i = 0, divisor = 1; i < DG_POWER; i++) divisor *= 1000;

            for (i = DG_POWER - 1; i >= 0; i--) {
                divisor /= 1000;
                mny = (int) (psum / divisor);
                psum %= divisor;
                //str="";
                if (mny == 0) {
                    if (i > 0) continue;
                    append(result, getPower(bundle, i, 0));
                } else {
                    if (mny >= 100) {
                        append(result, getDigit(bundle, mny / 100, hun));
                        mny %= 100;
                    }
                    if (mny >= 20) {
                        append(result, getDigit(bundle, mny / 10, dec));
                        mny %= 10;
                    }
                    if (mny >= 10) {
                        append(result, getDigit(bundle, mny - 10, dec2));
                    } else {
                        if (mny >= 1) append(result, getDigit(bundle, mny, isFemalePower(i) || female ? 1 : 0));
                    }

                    append(result, getPower(bundle, i, getVariation(mny)));
                }
            }
        } else append(result, bundle.getString("zero"));

        String postfix;
        if (numOfDigits != null && numOfDigits != 0)
            postfix = fractPostfix;
        else
            postfix = decPostfix;
        return (numericFraction ? appendZeroes(value) : result.toString()) + (postfix.isEmpty() ? "" : " ") + postfix;
    }

    private static void append(StringBuilder result, String value) {
        if(!value.isEmpty())
            result.append(result.length() == 0 ? "" : " ").append(value);
    }

    private static String appendZeroes(Long value) {
        String result = String.valueOf(value);
        while(result.length() < 2)
            result = "0" + result;
        return result;
    }

    private static int getVariation(Long value) {
        switch ((int) Math.abs(value) % 10) {
            case 1:
                if (String.valueOf(value).endsWith("11"))
                    return 2;
                else
                    return 0;
            case 2:
                if (String.valueOf(value).endsWith("12"))
                    return 2;
                else
                    return 1;
            case 3:
                if (String.valueOf(value).endsWith("13"))
                    return 2;
                else
                    return 1;
            case 4:
                if (String.valueOf(value).endsWith("14"))
                    return 2;
                else
                    return 1;
            default:
                return 2;
        }
    }

    private static int getVariation(int value) {
        switch (value) {
            case 1:
                return 0;
            case 2:
            case 3:
            case 4:
                return 1;
            default:
                return 2;
        }
    }

    private static int getNumOfDigits(BigDecimal number, String type, Integer forceNumOfDigits) {
        Integer numOfDigits = getNumOfDigits(type);
        if (numOfDigits == null) {
            numOfDigits = type.matches(".*\\d") ? Integer.parseInt(type.substring(type.length() - 1)) : getMinFractLength(number);
        }
        return Math.min(forceNumOfDigits == null ? numOfDigits : Math.max(numOfDigits, forceNumOfDigits), 6);
    }

    private static Integer getNumOfDigits(String type) {
        if(type != null) {
            switch (type) {
                case "BYN":
                case "RUB":
                case "EUR":
                case "USD":
                case "PLN":
                case "UAH":
                case "CNY":
                    return 2;
                case "ton":
                case "kg":
                case "gr":
                    return 3;
            }
        }
        return null;
    }

    private static BigDecimal getBigDecimal(Object number) {
        BigDecimal bigDecimal;
        if(number == null) {
            bigDecimal = BigDecimal.ZERO;
        } else if (number instanceof BigDecimal) {
            bigDecimal = (BigDecimal) number;
        } else if (number instanceof Double) {
            bigDecimal = BigDecimal.valueOf((Double) number);
        } else if (number instanceof Long) {
            bigDecimal = BigDecimal.valueOf((Long) number);
        } else if (number instanceof Integer) {
            bigDecimal = BigDecimal.valueOf((Integer) number);
        } else throw new RuntimeException("Unsupported class: " + number.getClass());
        return bigDecimal;
    }

    public static String capitalizeFirstLetter(String value) {
        if (value == null || value.isEmpty()) return null;
        else {
            char[] stringArray = value.toCharArray();
            stringArray[0] = Character.toUpperCase(stringArray[0]);
            return new String(stringArray);
        }
    }

    private static int getMinFractLength(BigDecimal number) {
        return number.abs().scale();
    }

    private static long getFract(BigDecimal number, int numOfDigits) {
        return numOfDigits == 0 ? 0 : number.remainder(BigDecimal.ONE).movePointRight(numOfDigits).abs().longValue();
    }

    private static String getPower(ResourceBundle bundle, int i, int variation) {
        if (i >= 1 && i <= 6) {
            return bundle.getString("power" + i + "." + variation);
        } else {
            return "";
        }
    }

    private static boolean isFemalePower(int i) {
        return i == 1; //thousand is female, others are mail
    }

    private static String getDecimalPostfix(ResourceBundle bundle, String type, int variation, boolean noDecimalPostfix) {
        if (type != null) {
            switch (type) {
                case "BYN":
                case "RUB":
                case "EUR":
                case "USD":
                case "PLN":
                case "UAH":
                case "CNY":
                case "ton":
                case "kg":
                case "gr":
                    return bundle.getString(type + "." + variation);
                case "number":
                    return noDecimalPostfix ? "" : bundle.getString(type + "." + variation);
                case "number0":
                    return "";
            }
        }
        return noDecimalPostfix ? "" : bundle.getString("number" + "." + variation);
    }

    private static String getFractalPostfix(ResourceBundle bundle, String type, int variation, int numOfDigits) {
        switch (type + numOfDigits) {
            case "BYN2":
            case "RUB2":
            case "EUR2":
            case "USD2":
            case "PLN2":
            case "UAH2":
            case "CNY2":
            case "ton3":
            case "kg3":
            case "gr3":
                return bundle.getString(type + numOfDigits + "." + variation);
            default:
                if (numOfDigits >= 1 && numOfDigits <= 6) {
                    return bundle.getString("number" + numOfDigits + "." + variation);
                } else {
                    return "";
                }
        }
    }

    private static String getDigit(ResourceBundle bundle, int digit, int variation) {
        return bundle.getString("digit" + digit + "." + variation);
    }

    private static boolean getSex(int power, String type, Integer numOfDigits, boolean female) {
        if(power > 0) {
            return isFemalePower(power);
        } else {
            if(type == null) {
                return false;
            } else {
                String fullType = type + (numOfDigits==null ? "" : numOfDigits);
                if(fullType.equals("number0")) {
                    return female;
                } else {
                    return getSex(fullType);

                }
            }
        }
    }

    private static boolean getSex(String type) { //true - female, false - male
        if (type != null) {
            switch (type) {
                case "ton":
                case "number":
                case "gr3":
                case "number1":
                case "number2":
                case "number3":
                case "number4":
                case "BYN2":
                case "RUB2":
                case "UAH":
                case "UAH2":
                    return true;
                case "kg":
                case "gr":
                case "ton3":
                case "kg3":
                case "number0":
                case "BYN":
                case "RUB":
                case "EUR":
                case "EUR2":
                case "USD":
                case "USD2":
                case "PLN":
                case "PLN2":
                case "CNY":
                case "CNY2":
                    return false;
            }
        }
        return false;
    }

    private static ResourceBundle getResourceBundle(String lang) {
        switch (lang) {
            case "by":
                return by;
            case "ru":
            default:
                return ru;
            case "ua":
                return ua;
        }
    }
}
