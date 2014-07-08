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

    public LCP singleTransaction;

    public ServiceLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(ServiceLogicsModule.class.getResourceAsStream("/lsfusion/system/Service.lsf"), "/lsfusion/system/Service.lsf", baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initProperties() throws RecognitionException {
        isServerRestarting = addProperty(null, new LCP<PropertyInterface>(new IsServerRestartingFormulaProperty("isServerRestarting")));
        super.initProperties();
        // Управление сервером базы данных
        checkAggregationsAction = findLAPByCompoundOldName("checkAggregationsAction");
        recalculateAction = findLAPByCompoundOldName("recalculateAction");
        recalculateFollowsAction = findLAPByCompoundOldName("recalculateFollowsAction");
        analyzeDBAction = findLAPByCompoundOldName("analyzeDBAction");
        packAction = findLAPByCompoundOldName("packAction");
        serviceDBAction = findLAPByCompoundOldName("serviceDBAction");
        singleTransaction = findLCPByCompoundOldName("singleTransaction");
    }
}
