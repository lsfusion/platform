package platform.base;

import java.text.NumberFormat;

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

    private final static String[][] fractalPostfix = new String[][]{
            {"десятая ", "десятых "},
            {"cотая ", "сотых "},
            {"тысячная ", "тысячных "},
            {"десятитысячная ", "десятитысячных "}
    };

    public static String toString(Integer number) {
        return toString(number.longValue(), null);
    }
    public static String toString(Long sumObject, Boolean isFractalPart, Integer numOfDigits) {

        long sum = sumObject == null ? 0 : sumObject;

        int i, mny;
        StringBuffer result = new StringBuffer("");
        long divisor; //делитель
        long psum = sum;

        int one = 1;
        int four = 2;
        int many = 3;

        int hun = 4;
        int dec = 3;
        int dec2 = 2;

        if (sum == 0) return "ноль ";
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
                    if (mny >= 1) result.append(digit[mny][1]);
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
                ;
            }
        }
        String postfix = "";
        Boolean endsWithOneButNotEleven = sumObject.toString().endsWith("1") && !sumObject.toString().endsWith("11");
        if (isFractalPart != null) {
            if (isFractalPart && sumObject < 10000)
                postfix = fractalPostfix[numOfDigits - 1][endsWithOneButNotEleven ? 0 : 1];
            else if (!isFractalPart)
                postfix = endsWithOneButNotEleven ? "целая " : "целых ";
        }
        return result.toString() + postfix;
    }

    public static String toString(Long sumObject, Boolean isFractalPart) {
        return toString(sumObject, isFractalPart, null);
    }

    public static String toString(Double numObject) {
        double num = numObject == null ? 0.0 : numObject;
        NumberFormat f = NumberFormat.getInstance();
        f.setGroupingUsed(false);
        f.setMaximumFractionDigits(4);
        int numOfDigits = f.format(num).length() - f.format((long) num).length() - 1;
        long fract = (long) (Math.round(num * Math.pow(10, numOfDigits)) - ((long) num) * Math.pow(10, numOfDigits));
        String result;
        if (fract != 0)
            result = toString((long) num, false) + toString(fract, true, numOfDigits);
        else
            result = toString((long) num, null);
        return result;
    }

}