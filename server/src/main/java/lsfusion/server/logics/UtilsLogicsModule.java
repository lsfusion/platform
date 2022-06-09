package lsfusion.server.logics;

import lsfusion.server.language.ScriptingLogicsModule;

import java.io.IOException;

public class UtilsLogicsModule extends ScriptingLogicsModule {

    public UtilsLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(baseLM, BL, "/system/Utils.lsf");
    }
}
