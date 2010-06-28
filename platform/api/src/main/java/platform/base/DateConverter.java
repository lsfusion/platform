package platform.base;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateConverter {

    public static java.sql.Date dateToInt(Date date) {
        if(date==null) return null;

        if(date instanceof java.sql.Date)
            return (java.sql.Date) date;
        else
            return new java.sql.Date(date.getTime());
    }

/*    private final static long MILLISECONDS_DAY = (1000*60*60*24);

    public static Integer dateToInt(Date date) {

        if (date == null) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        // переводим в GMT чтобы количество миллисекунд делилось нацело
        Calendar gmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        gmt.set(calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DATE));
        return (int)(gmt.getTimeInMillis() / MILLISECONDS_DAY);
    }
*/
    public static Date intToDate(java.sql.Date date) {
        return date;
    }

/*    public static Date intToDate(Integer num) {

        if (num == null) return null;

        Calendar gmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        gmt.setTimeInMillis(((long)num)* MILLISECONDS_DAY);

        Calendar calendar = Calendar.getInstance();
        calendar.set(gmt.get(Calendar.YEAR),gmt.get(Calendar.MONTH),gmt.get(Calendar.DATE));        
        return calendar.getTime();
    }*/


}
