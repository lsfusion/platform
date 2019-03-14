package lsfusion.server.logics.classes.utils.time;

import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.ConcreteCustomClass;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.data.Time;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.ScriptingLogicsModule;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.util.ArrayList;


public class TimeLogicsModule extends ScriptingLogicsModule{

    public ConcreteCustomClass month;
    public ConcreteCustomClass DOW;

    public LP currentDateTime;
    public LP toTime;
    public LP currentTime;
    public LP currentMinute;
    public LP currentHour;
    public LP currentEpoch;

    public LP extractYear;
    public LP currentDate;
    public LP currentMonth;

    public LP toDate;
    public LP sumDate;
    public LP subtractDate;

    public LP numberDOW;

    public TimeLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(TimeLogicsModule.class.getResourceAsStream("/system/Time.lsf"), "/system/Time.lsf", baseLM, BL);
    }

    @Override
    public void initMetaAndClasses() throws RecognitionException {
        super.initMetaAndClasses();
    }

    @Override
    public void initMainLogic() throws RecognitionException {

        currentDateTime = addTProp(LocalizedString.create("{logics.date.current.datetime}"), Time.DATETIME);
        makePropertyPublic(currentDateTime, "currentDateTime", new ArrayList<ResolveClassSet>());
        currentMinute = addTProp(LocalizedString.create("{logics.date.current.minute}"), Time.MINUTE);
        makePropertyPublic(currentMinute, "currentMinute", new ArrayList<ResolveClassSet>());
        currentHour = addTProp(LocalizedString.create("{logics.date.current.hour}"), Time.HOUR);
        makePropertyPublic(currentHour, "currentHour", new ArrayList<ResolveClassSet>());
        currentEpoch = addTProp(LocalizedString.create("{logics.date.current.epoch}"), Time.EPOCH);
        makePropertyPublic(currentEpoch, "currentEpoch", new ArrayList<ResolveClassSet>());

        super.initMainLogic();

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
