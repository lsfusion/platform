package lsfusion.server.logics;

import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;

public class UtilsLogicsModule extends ScriptingLogicsModule {

    public UtilsLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(TimeLogicsModule.class.getResourceAsStream("/system/Utils.lsf"), "/system/Utils.lsf", baseLM, BL);
    }
}
