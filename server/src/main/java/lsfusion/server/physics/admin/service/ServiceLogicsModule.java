package lsfusion.server.physics.admin.service;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.IsServerRestartingProperty;
import lsfusion.server.physics.dev.property.IsDevProperty;
import lsfusion.server.physics.dev.property.IsLightStartProperty;
import lsfusion.server.physics.dev.property.InTestModeProperty;
import lsfusion.server.physics.dev.property.ProjectLSFDirProperty;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.util.ArrayList;

public class ServiceLogicsModule extends ScriptingLogicsModule {

    public LA makeProcessDumpAction;

    public LP isServerRestarting;

    public LP singleTransaction;

    public LA recalculateMultiThreadAction;
    public LA recalculateClassesMultiThreadAction;
    public LA recalculateFollowsMultiThreadAction;
    public LA recalculateStatsMultiThreadAction;

    public LP overrideFocusedCellBorderColor;
    public LP overrideTableGridColor;

    public LP nameSetting;
    public LP overBaseValueSettingUserRole;

    public LP allowExcessAllocatedBytes;

    public LP transactTimeoutUser;
    public LP inDevMode;
    public LP isLightStart;

    public LP inTestMode;
    public LP projectLSFDir;

    public LP useKeystore;
    public LP keystorePassword;
    public LP keyPassword;
    public LP keystore;
    public LP privateKey;
    public LP chain;
    public LP privateKeyPassword;

    public ServiceLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(baseLM, BL, "/system/Service.lsf");
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        isServerRestarting = addProperty(null, new LP<>(new IsServerRestartingProperty()));
        makePropertyPublic(isServerRestarting, "isServerRestarting", new ArrayList<>());

        inDevMode = addProperty(null, new LP<>(IsDevProperty.instance));
        makePropertyPublic(inDevMode, "inDevMode", new ArrayList<>());
        isLightStart = addProperty(null, new LP<>(IsLightStartProperty.instance));
        makePropertyPublic(isLightStart, "isLightStart", new ArrayList<>());

        inTestMode = addProperty(null, new LP<>(InTestModeProperty.instance));
        makePropertyPublic(inTestMode, "inTestMode", new ArrayList<>());

        projectLSFDir = addProperty(null, new LP<>(ProjectLSFDirProperty.instance));
        makePropertyPublic(projectLSFDir, "projectLSFDir", new ArrayList<>());

        super.initMainLogic();
        // Управление сервером базы данных
        singleTransaction = findProperty("singleTransaction[]");

        makeProcessDumpAction = findAction("makeProcessDumpAction[]");

        recalculateMultiThreadAction = findAction("recalculateMultiThreadAction[]");
        recalculateClassesMultiThreadAction = findAction("recalculateClassesMultiThreadAction[]");
        recalculateFollowsMultiThreadAction = findAction("recalculateFollowsMultiThreadAction[]");
        recalculateStatsMultiThreadAction = findAction("recalculateStatsMultiThreadAction[]");

        overrideFocusedCellBorderColor = findProperty("overrideFocusedCellBorderColor[]");
        overrideTableGridColor = findProperty("overrideTableGridColor[]");

        nameSetting = findProperty("name[Setting]");
        overBaseValueSettingUserRole = findProperty("overBaseValue[Setting, UserRole]");

        allowExcessAllocatedBytes = findProperty("allowExcessAllocatedBytes[CustomUser]");

        transactTimeoutUser = findProperty("transactTimeout[User]");

        useKeystore = findProperty("useKeystore[]");
        keystorePassword = findProperty("keystorePassword[]");
        keyPassword = findProperty("keyPassword[]");
        keystore = findProperty("keystore[]");
        privateKey = findProperty("privateKey[]");
        chain = findProperty("chain[]");
        privateKeyPassword = findProperty("privateKeyPassword[]");
    }
}
