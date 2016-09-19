package lsfusion.server.profiler;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class ProfileValue {
    // nano
    public long totalTime = 0;
    public long totalSQLTime = 0;
    public long totalUserInteractionTime = 0;
    public double squaresSum = 0;
    public long callCount = 0;
    public long minTime = 0;
    public long maxTime = 0;
    
    public static ProfileValue merge(ProfileValue value1, ProfileValue value2) {
        ProfileValue result = new ProfileValue();
        result.totalTime = value1.totalTime + value2.totalTime;
        result.totalSQLTime = value1.totalSQLTime + value2.totalSQLTime;
        result.totalUserInteractionTime = value1.totalUserInteractionTime + value2.totalUserInteractionTime;
        result.squaresSum = value1.squaresSum + value2.squaresSum;
        result.callCount = value1.callCount + value2.callCount;
        result.minTime = min(value1.minTime, value2.minTime);
        result.maxTime = max(value1.maxTime, value2.maxTime);
        return result;
    }
    
    public void increase(long time, long sqlTime, long userInteractionTime) {
        totalTime += time;
        totalSQLTime += sqlTime;
        totalUserInteractionTime += userInteractionTime;
        squaresSum += (double) time * (double) time;
        callCount++;
        minTime = minTime == 0 ? time : min(minTime, time);
        maxTime = max(maxTime, time);
    }
}
