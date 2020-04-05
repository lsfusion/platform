package lsfusion.server.logics.classes.data.utils.time;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;


public class TimeLogicsModule extends ScriptingLogicsModule{

    public LP currentDateTime;
    public LP currentDateTimeSnapshot;
    public LP currentZDateTimeSnapshot;
    public LP currentDate;

    public TimeLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(TimeLogicsModule.class.getResourceAsStream("/system/Time.lsf"), "/system/Time.lsf", baseLM, BL);
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        super.initMainLogic();

        currentDateTime = findProperty("currentDateTime[]");
        currentDateTimeSnapshot = findProperty("currentDateTimeSnapshot[]");
        currentZDateTimeSnapshot = findProperty("currentZDateTimeSnapshot[]");
        currentDate = findProperty("currentDate[]");
    }
}
