package lsfusion.interop;

import java.io.Serializable;

public class TimePreferencies implements Serializable {
    public String userTimeZone;
    public Integer twoDigitYearStart;
    
    public TimePreferencies(String userTimeZone, Integer twoDigitYearStart) {
        this.userTimeZone = userTimeZone;
        this.twoDigitYearStart = twoDigitYearStart;
    }
}
