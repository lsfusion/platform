package platform.base;

import java.util.Calendar;
import java.util.Date;

public class DateConverter {

    public static Integer dateToInt(Date date) {

        if (date == null) return null;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return ((calendar.get(Calendar.YEAR) < 2000) ? -1 : 1) *(Math.abs(calendar.get(Calendar.YEAR) - 2000) * 10000 + calendar.get(Calendar.MONTH) * 100 + calendar.get(Calendar.DATE));
    }

    public static Date intToDate(Integer num) {

        if (num == null) return null;

        Calendar calendar = Calendar.getInstance();
        if (num < 0)
            calendar.set(2000 - (-num) / 10000, (-num / 100) % 100, -num % 100);
        else
            calendar.set(num / 10000 + 2000, (num / 100) % 100, num % 100);
        return calendar.getTime();
    }


}
