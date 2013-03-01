package platform.server.logics;

import org.antlr.runtime.RecognitionException;
import platform.server.classes.ConcreteCustomClass;
import platform.server.logics.linear.LCP;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;

public class UtilsLogicsModule extends ScriptingLogicsModule {

    public UtilsLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(UtilsLogicsModule.class.getResourceAsStream("/scripts/system/Utils.lsf"), baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();
    }
}
