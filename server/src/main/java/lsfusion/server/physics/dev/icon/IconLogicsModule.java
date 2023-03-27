package lsfusion.server.physics.dev.icon;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;

public class IconLogicsModule extends ScriptingLogicsModule {

    public LP<?> bestIconNames;
    public LA<?> getBestIcons;
    public LP<?> bestIconClasses;
    public LP<?> bestIconRanks;

    public LA<?> importIcons;

    public IconLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(baseLM, BL, "/system/Icon.lsf");
    }

    @Override
    public void initMetaAndClasses() throws RecognitionException {
        super.initMetaAndClasses();
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        super.initMainLogic();

        bestIconNames = findProperty("bestIconNames[STRING]");
        getBestIcons = findAction("getBestIcons[]");
        bestIconClasses = findProperty("bestIconClasses[STRING]");
        bestIconRanks = findProperty("bestIconRanks[STRING]");

        importIcons = findAction("importIcons[]");
    }
}
