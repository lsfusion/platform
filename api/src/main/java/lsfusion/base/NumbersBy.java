package lsfusion.base;

public class NumbersBy {

    private static String lang = "by";

    public static String toString(Object number) {
        return Numbers.toString(lang, number);
    }

    public static String toString(Object number, Integer numOfDigits) {
        return Numbers.toString(lang, number, numOfDigits);
    }

    public static String toString(Object number, boolean female) {
        return Numbers.toString(lang, number, female);
    }

    //main method without type
    public static String toString(Object number, Integer numOfDigits, boolean female, boolean upcase) {
        return Numbers.toString(lang, number, numOfDigits, female, upcase);
    }

    public static String toString(Object number, String type) {
        return Numbers.toString(lang, number, type);
    }

    public static String toString(Object number, String type, boolean upcase) {
        return Numbers.toString(lang, number, type, upcase);
    }

    public static String toString(Object number, String type, boolean numericFraction, boolean upcase) {
        return Numbers.toString(lang, number, type, numericFraction, upcase);
    }

    //main method with type
    public static String toString(Object number, String type, boolean numericFraction, Integer forceNumOfDigits, boolean upcase) {
        return Numbers.toString(lang, number, type, numericFraction, forceNumOfDigits, upcase);
    }

    public static String toStringCustom(Object numObject, String decPostfix, String fractPostfix) {
        return Numbers.toStringCustom(lang, numObject, decPostfix, fractPostfix);
    }

    public static String toStringCustom(Object numObject, String decPostfix, String fractPostfix, boolean numericFraction, boolean upcase) {
        return Numbers.toStringCustom(lang, numObject, decPostfix, fractPostfix, numericFraction, upcase);
    }

    //main method with custom postfixes
    public static String toStringCustom(Object numObject, String decPostfix, String fractPostfix, int numOfFractDigits, Integer forceNumOfFractDigits, boolean numericFraction, boolean upcase) {
        return Numbers.toStringCustom(lang, numObject, decPostfix, fractPostfix, numOfFractDigits, forceNumOfFractDigits, numericFraction, upcase);
    }
}
