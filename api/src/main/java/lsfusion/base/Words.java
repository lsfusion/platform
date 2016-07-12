package lsfusion.base;

import java.math.BigDecimal;
import java.util.HashMap;

public class Words {

    public final static int DG_POWER = 6;

    private final static String[][] a_power = new String[][]{
            {"0", "", "", ""},  // 1
            {"1", "тысяча ", "тысячи ", "тысяч "},  // 2
            {"0", "миллион ", "миллиона ", "миллионов "},  // 3
            {"0", "миллиард ", "миллиарда ", "миллиардов "},  // 4
            {"0", "триллион ", "триллиона ", "триллионов "},  // 5
            {"0", "квадриллион ", "квадриллиона ", "квадриллионов "},  // 6
            {"0", "квинтиллион ", "квинтиллиона ", "квинтиллионов "}   // 7
    };

    private final static String[][] digit = new String[][]{
            {"", "", "десять ", "", ""},
            {"один ", "одна ", "одиннадцать ", "десять ", "сто "},
            {"два ", "две ", "двенадцать ", "двадцать ", "двести "},
            {"три ", "три ", "тринадцать ", "тридцать ", "триста "},
            {"четыре ", "четыре ", "четырнадцать ", "сорок ", "четыреста "},
            {"пять ", "пять ", "пятнадцать ", "пятьдесят ", "пятьсот "},
            {"шесть ", "шесть ", "шестнадцать ", "шестьдесят ", "шестьсот "},
            {"семь ", "семь ", "семнадцать ", "семьдесят ", "семьсот "},
            {"восемь ", "восемь ", "восемнадцать ", "восемьдесят ", "восемьсот "},
            {"девять ", "девять ", "девятнадцать ", "девяносто ", "девятьсот "}
    };

    private static final HashMap<String, String[]> decimalPostfix = new HashMap<String, String[]>();

    static {
        decimalPostfix.put(("EUR"), new String[]{"евро ", "евро ", "евро "});
        decimalPostfix.put(("USD"), new String[]{"доллар США ", "доллара США ", "долларов США "});
        decimalPostfix.put(("RUB"), new String[]{"рубль ", "рубля ", "рублей "});
        decimalPostfix.put(("BLR"), new String[]{"белорусский рубль ", "белорусских рубля ", "белорусских рублей "});
        decimalPostfix.put(("ton"), new String[]{"тонна ", "тонны ", "тонн "});
        decimalPostfix.put(("kg"), new String[]{"килограмм ", "килограмма ", "килограмм "});
        decimalPostfix.put(("gr"), new String[]{"грамм ", "грамма ", "грамм "});
        decimalPostfix.put(("number0"), new String[]{"", "", ""});
        decimalPostfix.put(("number"), new String[]{"целая ", "целых ", "целых "});
    }

    private static final HashMap<String, String[]> fractalPostfix = new HashMap<String, String[]>();

    static {
        fractalPostfix.put("EUR2", new String[]{"евроцент ", "евроцента ", "евроцентов "});
        fractalPostfix.put("USD2", new String[]{"цент ", "цента ", "центов"});
        fractalPostfix.put("RUB2", new String[]{"копейка ", "копейки ", "копеек"});
        fractalPostfix.put("BLR2", new String[]{"копейка ", "копейки ", "копеек"});
        fractalPostfix.put("ton3", new String[]{"килограмм ", "килограмма ", "килограмм "});
        fractalPostfix.put("kg3", new String[]{"грамм ", "грамма ", "грамм "});
        fractalPostfix.put("gr3", new String[]{"тысячная ", "тысячных ", "тысячных "});
        fractalPostfix.put("number1", new String[]{"десятая ", "десятых ", "десятых "});
        fractalPostfix.put("number2", new String[]{"cотая ", "сотых ", "сотых "});
        fractalPostfix.put("number3", new String[]{"тысячная ", "тысячных ", "тысячных "});
        fractalPostfix.put("number4", new String[]{"десятитысячная ", "десятитысячных ", "десятитысячных "});
        fractalPostfix.put("number5", new String[]{"стотысячная ", "стотысячных ", "стотысячных "});
        fractalPostfix.put("number6", new String[]{"миллионная ", "миллионных ", "миллионных "});
    }

    private static final HashMap<String, Boolean> sexMap = new HashMap<String, Boolean>(); //true - female, false - male

    static {
        sexMap.put(("EUR"), false);
        sexMap.put(("USD"), false);
        sexMap.put(("RUB"), false);
        sexMap.put(("BLR"), false);
        sexMap.put(("ton"), true);
        sexMap.put(("kg"), false);
        sexMap.put(("gr"), false);
        sexMap.put(("number"), true);
        sexMap.put(("EUR2"), false);
        sexMap.put(("USD2"), false);
        sexMap.put(("RUB2"), true);
        sexMap.put(("BLR2"), true);
        sexMap.put(("ton3"), false);
        sexMap.put(("kg3"), false);
        sexMap.put(("gr3"), true);
        sexMap.put(("number0"), false);
        sexMap.put(("number1"), true);
        sexMap.put(("number2"), true);
        sexMap.put(("number3"), true);
        sexMap.put(("number4"), true);
    }

    private static final HashMap<String, Integer> numOfDigitsMap = new HashMap<String, Integer>(); //true - female, false - male

    static {
        numOfDigitsMap.put(("EUR"), 2);
        numOfDigitsMap.put(("USD"), 2);
        numOfDigitsMap.put(("RUB"), 2);
        numOfDigitsMap.put(("BLR"), 2);
        numOfDigitsMap.put(("ton"), 3);
        numOfDigitsMap.put(("kg"), 3);
        numOfDigitsMap.put(("gr"), 3);
    }

    private static String toString(Long value, String type, Integer numOfDigits, Boolean female) {

        long sum = value == null ? 0 : value;

        int i, mny;
        StringBuilder result = new StringBuilder("");
        long divisor; //делитель
        long psum = sum;

        int one = 1;
        int four = 2;
        int many = 3;

        int hun = 4;
        int dec = 3;
        int dec2 = 2;

        if (sum != 0) {
            if (sum < 0) {
                result.append("минус ");
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
                    result.append(a_power[i][one]);
                } else {
                    if (mny >= 100) {
                        result.append(digit[mny / 100][hun]);
                        mny %= 100;
                    }
                    if (mny >= 20) {
                        result.append(digit[mny / 10][dec]);
                        mny %= 10;
                    }
                    if (mny >= 10) {
                        result.append(digit[mny - 10][dec2]);
                    } else {
                        //int sex = 1;
                        //if (isFractalPart != null)
                        //    sex = isFractalPart ? 1 : ("0".equals(a_power[i][0]) ? 0 : 1);
                        String fullType = type + (numOfDigits==null ? "" : numOfDigits);
                        Integer sex = "1".equals(a_power[i][0]) ? 1 : (type == null ? 0 : (fullType.equals("number0") ? (female ? 1 : 0)
                                : (sexMap.containsKey (fullType) ? (sexMap.get(fullType) ? 1 : 0) : (sexMap.containsKey(type) ? (sexMap.get(type) ? 1 : 0) : 0))));
                        if (mny >= 1) result.append(digit[mny][(sex == null ? (female ? 1 : 0) : sex)]);
                    }
                    switch (mny) {
                        case 1:
                            result.append(a_power[i][one]);
                            break;
                        case 2:
                        case 3:
                        case 4:
                            result.append(a_power[i][four]);
                            break;
                        default:
                            result.append(a_power[i][many]);
                            break;
                    }
                }
            }
        } else result.append("ноль ");

        String postfix;
        String decPostfix = decimalPostfix.containsKey(type) ?
                decimalPostfix.get(type)[getVariation(value)] :
                decimalPostfix.get("number")[getVariation(value)];
        if (numOfDigits != null) {
            if (numOfDigits != 0)
                postfix = fractalPostfix.containsKey(type) ?
                        fractalPostfix.get(type)[getVariation(value)] :
                        fractalPostfix.containsKey(type + numOfDigits) ?
                                fractalPostfix.get(type + numOfDigits)[getVariation(value)] :
                                fractalPostfix.get("number" + numOfDigits)[getVariation(value)];
            else
                postfix = decPostfix;
        } else
            postfix = type.equals("number0") ? decimalPostfix.get("number0")[getVariation(value)] : decPostfix;
        return result.toString() + postfix;
    }

    private static int getVariation(long value) {
        switch ((int) value % 10) {
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

    public static String getWord(String value, int index) {
        if ((value == null) || (index < 0))
            return "";
        String[] splitValue = value.split(",");
        if (splitValue.length <= index)
            return "";
        else return splitValue[index];
    }

    private static int getNumOfDigits(double num, String type) {
        if (type != null) {
            if (numOfDigitsMap.containsKey(type))
                return numOfDigitsMap.get(type);
            else if (type.matches(".*\\d"))
                return Integer.parseInt(type.substring(type.length() - 1, type.length()));
        }
        int numOfDigits = 0;
        while (Math.abs(num - Math.round(num)) > 1E-7) {
            numOfDigits++;
            num = num * 10;
        }
        return Math.min(numOfDigits, 6);
    }

    //для лонга с типом
    public static String toString(Long number, String type) {
        if (decimalPostfix.containsKey(type))
            return toString(number, type, null, sexMap.get(type));
        else return toString(number, false);
    }

    //для лонга без типа
    public static String toString(Long number, Boolean female) {
        return toString(number, "number0", null, female);
    }

    //для интежера с типом
    public static String toString(Integer number, String type) {
        if (decimalPostfix.containsKey(type))
            return toString(number.longValue(), type, null, sexMap.get(type));
        else return toString(number, false);
    }

    //для интежера без типа
    public static String toString(Integer number, Boolean female) {
        return toString(number.longValue(), "number0", null, female);
    }

    public static String toString(Integer number) {
        return toString(number, false);
    }

    //для дабла с типом
    public static String toString(Double numObject, String type) {
        double num = numObject == null ? 0.0 : numObject;
        Integer numOfDigits = getNumOfDigits(num, type);
        if (decimalPostfix.containsKey(type) && (fractalPostfix.containsKey(type) || fractalPostfix.containsKey(type + numOfDigits))) {
            long fract = Math.round(num * Math.pow(10, numOfDigits) - ((long) num) * Math.pow(10, numOfDigits));
            String result;
            if (fract != 0)
                result = toString((long) num, type, null, sexMap.get(type)) + toString(fract, type, numOfDigits, sexMap.get(type + numOfDigits));
            else
                result = toString((long) num, type);
            return result;
        } else
            return toString(numObject, numOfDigits, true);
    }

    //для дабла без типа
    public static String toString(Double numObject, Integer numOfDigits, Boolean female) {
        double num = numObject == null ? 0.0 : numObject;
        int numOfDig = numOfDigits == null ? 0 : Math.min(numOfDigits, 6);
        long fract = numOfDig == 0 ? 0 : (long) (Math.round(num * Math.pow(10, numOfDig)) - ((long) num) * Math.pow(10, numOfDig));
        String result;
        if (fract != 0)
            result = toString((long) num, "number", 0, female) + toString(fract, "number" + numOfDig, numOfDig, female);
        else
            result = toString((long) num, "number0");
        return result;
    }

    public static String toString(Double numObject, Integer numOfDigits) {
        return toString(numObject, numOfDigits, false);
    }

    public static String toString(Double numObject, Boolean female) {
        double num = numObject == null ? 0.0 : numObject;
        int numOfDig = 0;
        while (Math.abs(num - Math.round(num)) > 1E-7) { 
            numOfDig++;
            num = num * 10;
        }
        return toString(numObject, Math.min(numOfDig, 6), female);

    }

    public static String toString(Double numObject) {
        return toString(numObject, false);
    }

    public static String toString(BigDecimal numObject, String type) {
        if (numObject == null) return toString((Double) null, type);
        return toString(numObject.doubleValue(), type);
    }

    public static String toString(BigDecimal numObject, String type, boolean upcase) {
        String result = toString(numObject, type);
        if (result != null && upcase)
            result = result.substring(0, 1).toUpperCase() + result.substring(1);
        return result;
    }

    //для дабла без типа
    public static String toString(BigDecimal numObject, Integer numOfDigits, Boolean female) {
        if (numObject == null) return toString((Double) null, numOfDigits, female);
        return toString(numObject.doubleValue(), numOfDigits, female);
    }

    public static String toString(BigDecimal numObject, Integer numOfDigits) {
        if (numObject == null) return toString((Double) null, numOfDigits);
        return toString(numObject.doubleValue(), numOfDigits);
    }

    public static String toString(BigDecimal numObject, Boolean female) {
        if (numObject == null) return toString((Double) null, female);
        return toString(numObject.doubleValue(), female);
    }

    public static String toString(BigDecimal numObject) {
        return toString(numObject, false);
    }

    public static String capitalizeFirstLetter(String value) {
        if (value == null || value.isEmpty()) return null;
        else {
            char[] stringArray = value.toCharArray();
            stringArray[0] = Character.toUpperCase(stringArray[0]);
            return new String(stringArray);
        }
    }
}
