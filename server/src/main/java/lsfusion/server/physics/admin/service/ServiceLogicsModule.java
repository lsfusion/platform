package lsfusion.server.physics.admin.service;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.IsServerRestartingProperty;
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
    public LP overrideSelectedCellBackgroundColor;
    public LP overrideFocusedCellBackgroundColor;
    public LP overrideFocusedCellBorderColor;
    public LP overrideTableGridColor;

    public LP nameSetting;
    public LP overBaseValueSettingUserRole;

    public ServiceLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(ServiceLogicsModule.class.getResourceAsStream("/system/Service.lsf"), "/system/Service.lsf", baseLM, BL);
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        isServerRestarting = addProperty(null, new LP<>(new IsServerRestartingProperty()));
        makePropertyPublic(isServerRestarting, "isServerRestarting", new ArrayList<>());
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
        overrideSelectedCellBackgroundColor = findProperty("overrideSelectedCellBackgroundColor[]");
        overrideFocusedCellBackgroundColor = findProperty("overrideFocusedCellBackgroundColor[]");
        overrideFocusedCellBorderColor = findProperty("overrideFocusedCellBorderColor[]");
        overrideTableGridColor = findProperty("overrideTableGridColor[]");

        nameSetting = findProperty("name[Setting]");
        overBaseValueSettingUserRole = findProperty("overBaseValue[Setting, UserRole]");
    }
}
