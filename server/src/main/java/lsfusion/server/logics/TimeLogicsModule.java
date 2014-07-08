package lsfusion.server.logics;

import org.antlr.runtime.RecognitionException;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.data.Time;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;

import static lsfusion.server.logics.ServerResourceBundle.getString;


public class TimeLogicsModule extends ScriptingLogicsModule{

    public ConcreteCustomClass month;
    public ConcreteCustomClass DOW;

    public LCP currentDateTime;
    public LCP toTime;
    public LCP currentTime;
    public LCP currentMinute;
    public LCP currentHour;
    public LCP currentEpoch;

    public LCP extractYear;
    public LCP currentDate;
    public LCP currentMonth;

    public LCP toDate;
    public LCP sumDate;
    public LCP subtractDate;

    public TimeLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(TimeLogicsModule.class.getResourceAsStream("/lsfusion/system/Time.lsf"), "/lsfusion/system/Time.lsf", baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();
    }

    @Override
    public void initProperties() throws RecognitionException {

        currentDateTime = addTProp("currentDateTime", getString("logics.date.current.datetime"), Time.DATETIME);
        currentMinute = addTProp("currentMinute", getString("logics.date.current.minute"), Time.MINUTE);
        currentHour = addTProp("currentHour", getString("logics.date.current.hour"), Time.HOUR);
        currentEpoch = addTProp("currentEpoch", getString("logics.date.current.epoch"), Time.EPOCH);

        super.initProperties();

        month = (ConcreteCustomClass) getClass("Month");
        DOW = (ConcreteCustomClass) getClass("DOW");

        extractYear = findLCPByCompoundOldName("extractYear");
        currentDate = findLCPByCompoundOldName("currentDate");
        currentMonth = findLCPByCompoundOldName("currentMonth");

        toDate = findLCPByCompoundOldName("toDate");
        toTime = findLCPByCompoundOldName("toTime");
        sumDate = findLCPByCompoundOldName("sumDate");
        subtractDate = findLCPByCompoundOldName("subtractDate");

        currentTime = findLCPByCompoundOldName("currentTime");
    }
}
