package lsfusion.server.logics;

import org.antlr.runtime.RecognitionException;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.IsServerRestartingFormulaProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

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
        checkAggregationsAction = getLAPByOldName("checkAggregationsAction");
        recalculateAction = getLAPByOldName("recalculateAction");
        recalculateFollowsAction = getLAPByOldName("recalculateFollowsAction");
        analyzeDBAction = getLAPByOldName("analyzeDBAction");
        packAction = getLAPByOldName("packAction");
        serviceDBAction = getLAPByOldName("serviceDBAction");
    }
}
