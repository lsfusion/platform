package platform.server.logics;

import org.antlr.runtime.RecognitionException;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.IsServerRestartingFormulaProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;

public class ServiceLogicsModule extends ScriptingLogicsModule {

    private LAP checkAggregationsAction;
    private LAP recalculateAction;
    private LAP recalculateFollowsAction;
    private LAP analyzeDBAction;
    private LAP packAction;
    private LAP serviceDBAction;

    public LCP isServerRestarting;
    public LAP restartServerAction;
    public LAP runGarbageCollector;
    public LAP cancelRestartServerAction;
    
    public ServiceLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(ServiceLogicsModule.class.getResourceAsStream("/scripts/system/Service.lsf"), baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initProperties() throws RecognitionException {
        isServerRestarting = addProperty(null, new LCP<PropertyInterface>(new IsServerRestartingFormulaProperty("isServerRestarting")));
        super.initProperties();
        // Управление сервером базы данных
        checkAggregationsAction = getLAPByName("checkAggregationsAction");
        recalculateAction = getLAPByName("recalculateAction");
        recalculateFollowsAction = getLAPByName("recalculateFollowsAction");
        analyzeDBAction = getLAPByName("analyzeDBAction");
        packAction = getLAPByName("packAction");
        serviceDBAction = getLAPByName("serviceDBAction");

        // Управление сервером приложений
        restartServerAction = getLAPByName("RestartAction");
        runGarbageCollector = getLAPByName("GarbageCollectorAction");
        cancelRestartServerAction = getLAPByName("CancelRestartAction");
    }
}
