package platform.server.logics;

import org.antlr.runtime.RecognitionException;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.TimeClass;
import platform.server.data.Time;
import platform.server.logics.linear.LCP;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;

import static platform.server.logics.ServerResourceBundle.getString;


public class TimeLogicsModule extends ScriptingLogicsModule{

    public ConcreteCustomClass month;
    public ConcreteCustomClass DOW;

    public LCP currentDateTime;
    public LCP toTime;
    public LCP currentTime;
    public LCP currentMinute;
    public LCP currentHour;
    public LCP currentEpoch;

    public LCP weekInDate;
    public LCP yearInDate;
    public LCP numberMonthInDate;
    public LCP currentDate;
    public LCP currentMonth;

    public LCP toDate;
    public LCP sumDate;
    public LCP subtractDate;

    public TimeLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(TimeLogicsModule.class.getResourceAsStream("/scripts/system/Time.lsf"), baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();
    }

    @Override
    public void initProperties() throws RecognitionException {

        super.initProperties();

        month = (ConcreteCustomClass) getClassByName("Month");
        DOW = (ConcreteCustomClass) getClassByName("DOW");

        weekInDate = getLCPByName("weekInDate");
        yearInDate = getLCPByName("yearInDate");
        numberMonthInDate = getLCPByName("numberMonthInDate");
        currentDate = getLCPByName("currentDate");
        currentMonth = getLCPByName("currentMonth");

        toDate = getLCPByName("toDate");
        toTime = getLCPByName("toTime");
        sumDate = getLCPByName("sumDate");
        subtractDate = getLCPByName("subtractDate");

        currentDateTime = addTProp("currentDateTime", getString("logics.date.current.datetime"), Time.DATETIME);
        currentTime = addJProp("currentTime", getString("logics.date.current.time"), toTime, currentDateTime);
        currentMinute = addTProp("currentMinute", getString("logics.date.current.minute"), Time.MINUTE);
        currentHour = addTProp("currentHour", getString("logics.date.current.hour"), Time.HOUR);
        currentEpoch = addTProp("currentEpoch", getString("logics.date.current.epoch"), Time.EPOCH);
    }
}
