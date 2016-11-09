package lsfusion.server.logics;

import lsfusion.server.classes.sets.ResolveClassSet;
import org.antlr.runtime.RecognitionException;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.data.Time;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.util.ArrayList;

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

    public LCP numberDOW;

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

        currentDateTime = addTProp(getString("logics.date.current.datetime"), Time.DATETIME);
        makePropertyPublic(currentDateTime, "currentDateTime", new ArrayList<ResolveClassSet>());
        currentMinute = addTProp(getString("logics.date.current.minute"), Time.MINUTE);
        makePropertyPublic(currentMinute, "currentMinute", new ArrayList<ResolveClassSet>());
        currentHour = addTProp(getString("logics.date.current.hour"), Time.HOUR);
        makePropertyPublic(currentHour, "currentHour", new ArrayList<ResolveClassSet>());
        currentEpoch = addTProp(getString("logics.date.current.epoch"), Time.EPOCH);
        makePropertyPublic(currentEpoch, "currentEpoch", new ArrayList<ResolveClassSet>());

        super.initProperties();

        month = (ConcreteCustomClass) findClass("Month");
        DOW = (ConcreteCustomClass) findClass("DOW");

        extractYear = findProperty("extractYear[?]");
        currentDate = findProperty("currentDate[]");
        currentMonth = findProperty("currentMonth[]");

        toDate = findProperty("toDate[DATETIME]");
        toTime = findProperty("toTime[DATETIME]");
        sumDate = findProperty("sum[DATE,LONG]");
        subtractDate = findProperty("subtract[DATE,LONG]");

        numberDOW = findProperty("number[DOW]");

        currentTime = findProperty("currentTime[]");
    }
}
