package platform.base;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateConverter {

    private final static long MILLISECONDS_DAY = (1000*60*60*24);

    public static Integer dateToInt(Date date) {

        if (date == null) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        // переводим в GMT чтобы количество миллисекунд делилось нацело
        Calendar gmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        gmt.set(calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DATE));
        return (int)(gmt.getTimeInMillis() / MILLISECONDS_DAY);
/*
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return ((calendar.get(Calendar.YEAR) < 2000) ? -1 : 1) *(Math.abs(calendar.get(Calendar.YEAR) - 2000) * 10000 + calendar.get(Calendar.MONTH) * 100 + calendar.get(Calendar.DATE));
        */
    }

    public static Date intToDate(Integer num) {

        if (num == null) return null;

        Calendar gmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        gmt.setTimeInMillis(((long)num)* MILLISECONDS_DAY);

        Calendar calendar = Calendar.getInstance();
        calendar.set(gmt.get(Calendar.YEAR),gmt.get(Calendar.MONTH),gmt.get(Calendar.DATE));        
        return calendar.getTime();
/*        Calendar calendar = Calendar.getInstance();
        if (num < 0)
            calendar.set(2000 - (-num) / 10000, (-num / 100) % 100, -num % 100);
        else
            calendar.set(num / 10000 + 2000, (num / 100) % 100, num % 100);
        return calendar.getTime();*/
    }


}
