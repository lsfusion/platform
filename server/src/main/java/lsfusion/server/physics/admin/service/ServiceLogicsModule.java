package lsfusion.server.physics.admin.service;

import lsfusion.server.language.linear.LA;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.property.env.IsServerRestartingFormulaProperty;
import lsfusion.server.language.ScriptingLogicsModule;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.util.ArrayList;

public class ServiceLogicsModule extends ScriptingLogicsModule {

    private LA checkAggregationsAction;
    private LA recalculateAction;
    private LA recalculateFollowsAction;
    private LA analyzeDBAction;
    private LA packAction;
    private LA serviceDBAction;

    public LA makeProcessDumpAction;

    public LP isServerRestarting;
    public LA restartServerAction;
    public LA runGarbageCollector;
    public LA cancelRestartServerAction;

    public LP singleTransaction;

    public LA recalculateMultiThreadAction;
    public LA recalculateClassesMultiThreadAction;
    public LA recalculateFollowsMultiThreadAction;
    public LA recalculateStatsMultiThreadAction;

    public LP overrideSelectedRowBackgroundColor;
    public LP overrideSelectedRowBorderColor;
    public LP overrideSelectedCellBackgroundColor;
    public LP overrideFocusedCellBackgroundColor;
    public LP overrideFocusedCellBorderColor;

    public LP nameSetting;
    public LP overBaseValueSettingUserRole;

    public ServiceLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(ServiceLogicsModule.class.getResourceAsStream("/system/Service.lsf"), "/system/Service.lsf", baseLM, BL);
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        isServerRestarting = addProperty(null, new LP<>(new IsServerRestartingFormulaProperty()));
        makePropertyPublic(isServerRestarting, "isServerRestarting", new ArrayList<ResolveClassSet>());
        super.initMainLogic();
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

        nameSetting = findProperty("name[Setting]");
        overBaseValueSettingUserRole = findProperty("overBaseValue[Setting, UserRole]");
    }
}
