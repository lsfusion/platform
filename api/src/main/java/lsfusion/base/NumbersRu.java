package lsfusion.base;

/**
 * Класс NumbersRu содержит методы для перевода чисел в прописной вид на русском языке
 * Методы в этом классе подразделяются на 3 группы:
 * <p><b>1. Без передачи типа</b><br>
 * toString(123.45) -> сто двадцать три целых сорок пять сотых<br>
 * toString(123.45, 4) -> сто двадцать три целых четыре тысячи пятьсот десятитысячных<br>
 * toString(1, false) -> один<br>
 * toString(1, true) -> одна<br>
 * toString(12.34, 3, false, true) -> Двенадцать целых триста сорок тысячных</p>
 *
 * <p><b>2. С передачей типа</b><br>
 * toString(12.34, "BYN") -> двенадцать белорусских рублей тридцать четыре копейки<br>
 * NumbersRu.toString(23.456, "kg", true) -> Двадцать три килограмма четыреста пятьдесят шесть грамм<br>
 * NumbersRu.toString(56.02, "USD", true, false) -> пятьдесят шесть долларов США 02 цента<br>
 * toString(999, "RUB", false, 2, true) -> Девятьсот девяносто девять российских рублей ноль копеек</p>
 *
 * <p><b>3. С передачей постфиксов целой и дробной части</b><br>
 * toStringCustom(123.45, "руб", "коп") -> сто двадцать три руб сорок пять коп<br>
 * toStringCustom(44.12, "руб", "коп", true, true) -> Сорок четыре руб 12 коп<br>
 * toStringCustom(12, "кг", "гр", 3, 3, false, true); -> Двенадцать кг двести гр<br>
 * </p>
 */

public class NumbersRu {

    private static String lang = "ru";

    public static String toString(Object number) {
        return Numbers.toString(lang, number);
    }

    public static String toString(Object number, Integer numOfDigits) {
        return Numbers.toString(lang, number, numOfDigits);
    }

    public static String toString(Object number, boolean female) {
        return Numbers.toString(lang, number, female);
    }

    /**
     * группа методов toString без передачи типа
     *
     * @param number
     * Число для преобразования. Поддерживаются типы BigDecimal, Double, Long, Integer
     *
     * @param numOfDigits
     * Количество знаков после запятой.
     * Если не указано, то минимально необходимое для отображения числа без округления, но не больше 6.
     *
     * @param female
     * True указывает на то, что результат должен иметь женский род (двадцать одна, а не двадцать один).
     * По умолчанию False.
     *
     * @param upcase
     * Первое слово с большой буквы.
     * По умолчанию False.
     */
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

    /**
     * группа методов toString с передачей типа
     *
     * @param number
     * Число для преобразования. Поддерживаются типы BigDecimal, Double, Long, Integer
     *
     * @param type
     * Тип данных: "EUR" (евро), "USD" (доллар США), "BYN" (белорусский рубль), "RUB" (российский рубль), "PLN" (злотый), "UAH" (гривна), "CNY" (юань), "ton" (тонна), "kg" (килограмм), "gr" (грамм), "number" (число), "number0"..."number6" (число с фиксированным количеством знаков после запятой. По умолчанию "number".
     *
     * @param numericFraction
     * True указывает на то, что дробная часть результата возвращается не прописью, а цифрами.
     * По умолчанию False.
     *
     * @param forceNumOfDigits
     * Обязательное кол-во знаков после запятой. По умолчанию, не применяется.
     *
     * @param upcase
     * Первое слово с большой буквы.
     * По умолчанию False.
     */
    public static String toString(Object number, String type, boolean numericFraction, Integer forceNumOfDigits, boolean upcase) {
        return Numbers.toString(lang, number, type, numericFraction, forceNumOfDigits, upcase);
    }

    public static String toStringCustom(Object number, String decPostfix, String fractPostfix) {
        return Numbers.toStringCustom(lang, number, decPostfix, fractPostfix);
    }

    public static String toStringCustom(Object number, String decPostfix, String fractPostfix, boolean numericFraction, boolean upcase) {
        return Numbers.toStringCustom(lang, number, decPostfix, fractPostfix, numericFraction, upcase);
    }

    /**
     * группа методов toStringCustom с передачей постфиксов целой и дробной части
     *
     * @param number
     * Число для преобразования. Поддерживаются типы BigDecimal, Double, Long, Integer
     *
     * @param decPostfix
     * Постфикс целой части. Например, "руб."
     *
     * @param fractPostfix
     * Постфикс дробной части. Например, "коп."
     *
     * @param numOfFractDigits
     * Кол-во знаков после запятой. По умолчанию, 2.
     *
     * @param forceNumOfFractDigits
     * Обязательное кол-во знаков после запятой. По умолчанию, не применяется.
     *
     * @param numericFraction
     * True указывает на то, что дробная часть результата возвращается не прописью, а цифрами.
     * По умолчанию False.
     *
     * @param upcase
     * Первое слово с большой буквы.
     * По умолчанию False.
     */
    public static String toStringCustom(Object number, String decPostfix, String fractPostfix, int numOfFractDigits, Integer forceNumOfFractDigits, boolean numericFraction, boolean upcase) {
        return Numbers.toStringCustom(lang, number, decPostfix, fractPostfix, numOfFractDigits, forceNumOfFractDigits, numericFraction, upcase);
    }
}
