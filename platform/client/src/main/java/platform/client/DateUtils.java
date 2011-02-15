package platform.client;

import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public static String[] months = new String[] {"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"};

    public static String formatRussian(Date date) {

        // todo : сделать форматирование по timeZone сервера

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return "" + calendar.get(Calendar.DAY_OF_MONTH) + " " + months[calendar.get(Calendar.MONTH)] + " " + calendar.get(Calendar.YEAR);
    }
}
