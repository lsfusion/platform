package lsfusion.server.physics.admin.service;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.IsServerRestartingProperty;
import lsfusion.server.physics.dev.property.IsDevProperty;
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

    public LP useBusyDialog;
    public LP useRequestTimeout;
    public LP devMode;

    public LP allowExcessAllocatedBytes;

    public LP transactTimeoutUser;
    public LP inDevMode;

    public LA readMainResources;
    public LP mainResources;

    public ServiceLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(baseLM, BL, "/system/Service.lsf");
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        isServerRestarting = addProperty(null, new LP<>(new IsServerRestartingProperty()));
        makePropertyPublic(isServerRestarting, "isServerRestarting", new ArrayList<>());

        inDevMode = addProperty(null, new LP<>(IsDevProperty.instance));
        makePropertyPublic(inDevMode, "inDevMode", new ArrayList<>());
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

        useBusyDialog = findProperty("useBusyDialog[]");
        useRequestTimeout = findProperty("useRequestTimeout[]");
        devMode = findProperty("devMode[]");

        allowExcessAllocatedBytes = findProperty("allowExcessAllocatedBytes[CustomUser]");

        transactTimeoutUser = findProperty("transactTimeout[User]");

        readMainResources = findAction("readMainResources[]");
        mainResources = findProperty("mainResources[]");
    }
}
