package lsfusion.server.logics;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.classes.utils.time.TimeLogicsModule;

import java.io.IOException;

public class UtilsLogicsModule extends ScriptingLogicsModule {

    public UtilsLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(TimeLogicsModule.class.getResourceAsStream("/system/Utils.lsf"), "/system/Utils.lsf", baseLM, BL);
    }
}
