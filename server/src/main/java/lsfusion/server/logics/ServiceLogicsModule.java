package lsfusion.server.logics;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.IsServerRestartingFormulaProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.util.ArrayList;

public class ServiceLogicsModule extends ScriptingLogicsModule {

    private LAP checkAggregationsAction;
    private LAP recalculateAction;
    private LAP recalculateFollowsAction;
    private LAP analyzeDBAction;
    private LAP packAction;
    private LAP serviceDBAction;

    public LAP makeProcessDumpAction;

    public LCP isServerRestarting;
    public LAP restartServerAction;
    public LAP runGarbageCollector;
    public LAP cancelRestartServerAction;

    public LCP singleTransaction;

    public LAP recalculateMultiThreadAction;
    public LAP recalculateClassesMultiThreadAction;
    public LAP recalculateFollowsMultiThreadAction;
    public LAP recalculateStatsMultiThreadAction;

    public LCP overrideSelectedRowBackgroundColor;
    public LCP overrideSelectedRowBorderColor;
    public LCP overrideSelectedCellBackgroundColor;
    public LCP overrideFocusedCellBackgroundColor;
    public LCP overrideFocusedCellBorderColor;

    public LCP nameReflectionProperty;
    public LCP overBaseValueReflectionPropertyUserRole;

    public ServiceLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(ServiceLogicsModule.class.getResourceAsStream("/lsfusion/system/Service.lsf"), "/lsfusion/system/Service.lsf", baseLM, BL);
    }

    @Override
    public void initProperties() throws RecognitionException {
        isServerRestarting = addProperty(null, new LCP<>(new IsServerRestartingFormulaProperty()));
        makePropertyPublic(isServerRestarting, "isServerRestarting", new ArrayList<ResolveClassSet>());
        super.initProperties();
        // Управление сервером базы данных
        checkAggregationsAction = findAction("checkAggregationsAction[]");
        recalculateAction = findAction("recalculateAction[]");
        recalculateFollowsAction = findAction("recalculateFollowsAction[]");
        analyzeDBAction = findAction("analyzeDBAction[]");
        packAction = findAction("packAction[]");
        serviceDBAction = findAction("serviceDBAction[]");
        singleTransaction = findProperty("singleTransaction[]");

        makeProcessDumpAction = findAction("makeProcessDumpAction[]");

        recalculateMultiThreadAction = findAction("recalculateMultiThreadAction[]");
        recalculateClassesMultiThreadAction = findAction("recalculateClassesMultiThreadAction[]");
        recalculateFollowsMultiThreadAction = findAction("recalculateFollowsMultiThreadAction[]");
        recalculateStatsMultiThreadAction = findAction("recalculateStatsMultiThreadAction[]");

        overrideSelectedRowBackgroundColor = findProperty("overrideSelectedRowBackgroundColor[]");
        overrideSelectedRowBorderColor = findProperty("overrideSelectedRowBorderColor[]");
        overrideSelectedCellBackgroundColor = findProperty("overrideSelectedCellBackgroundColor[]");
        overrideFocusedCellBackgroundColor = findProperty("overrideFocusedCellBackgroundColor[]");
        overrideFocusedCellBorderColor = findProperty("overrideFocusedCellBorderColor[]");

        nameReflectionProperty = findProperty("name[ReflectionProperty]");
        overBaseValueReflectionPropertyUserRole = findProperty("overBaseValue[ReflectionProperty, UserRole]");
    }
}
