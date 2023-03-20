package lsfusion.server.physics.dev.icon;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;

public class IconLogicsModule extends ScriptingLogicsModule {

    public LA getBestIcon;
    public LP bestIconClass;
    public LP bestIconRank;

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

        getBestIcon = findAction("getBestIcon[STRING]");
        bestIconClass = findProperty("bestIconClass[]");
        bestIconRank = findProperty("bestIconRank[]");

    }
}
