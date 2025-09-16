package lsfusion.server.physics.admin.service;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.property.IsServerRestartingProperty;
import lsfusion.server.physics.admin.service.property.CurrentAppServerProperty;
import lsfusion.server.physics.dev.property.IsDevProperty;
import lsfusion.server.physics.dev.property.IsLightStartProperty;
import lsfusion.server.physics.dev.property.InTestModeProperty;
import lsfusion.server.physics.dev.property.ProjectLSFDirProperty;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;

public class ServiceLogicsModule extends ScriptingLogicsModule {
    public ConcreteCustomClass appServer;

    public LA makeProcessDumpAction;

    public LP isServerRestarting;

    public LP singleTransaction;

    public LA recalculateMultiThreadAction;
    public LA recalculateClassesMultiThreadAction;
    public LA recalculateFollowsMultiThreadAction;
    public LA recalculateStatsMultiThreadAction;

    public LP overrideFocusedCellBorderColor;
    public LP overrideTableGridColor;

    public LP computerSettings;

    public LP nameSetting;
    public LP overBaseValueSettingUserRole;

    public ConcreteCustomClass dbSlave;
    public LP<?> hostDBSlave;

    public LP allowExcessAllocatedBytes;

    public LP transactTimeoutUser;
    public LP inDevMode;
    public LP isLightStart;

    public LP inTestMode;
    public LP projectLSFDir;

    public LP currentAppServer;
    public LP appServerConnectionString;
    public LP appServerByConnectionString;
    public LP isStartedAppServer;
    
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
    public void initMetaAndClasses() throws RecognitionException {
        super.initMetaAndClasses();

        appServer = (ConcreteCustomClass) findClass("AppServer");
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        currentAppServer = addProperty(null, new LP<>(new CurrentAppServerProperty(appServer)));
        makePropertyPublic(currentAppServer, "currentAppServer");
        
        isServerRestarting = addProperty(null, new LP<>(new IsServerRestartingProperty()));
        makePropertyPublic(isServerRestarting, "isServerRestarting");

        inDevMode = addProperty(null, new LP<>(IsDevProperty.instance));
        makePropertyPublic(inDevMode, "inDevMode");
        isLightStart = addProperty(null, new LP<>(IsLightStartProperty.instance));
        makePropertyPublic(isLightStart, "isLightStart");

        inTestMode = addProperty(null, new LP<>(InTestModeProperty.instance));
        makePropertyPublic(inTestMode, "inTestMode");

        projectLSFDir = addProperty(null, new LP<>(ProjectLSFDirProperty.instance));
        makePropertyPublic(projectLSFDir, "projectLSFDir");

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

        computerSettings = findProperty("computerSettings[]");

        nameSetting = findProperty("name[Setting]");
        overBaseValueSettingUserRole = findProperty("overBaseValue[Setting, UserRole]");

        dbSlave = (ConcreteCustomClass) findClass("DBSlave");
        hostDBSlave = findProperty("host[DBSlave]");
        allowExcessAllocatedBytes = findProperty("allowExcessAllocatedBytes[CustomUser]");

        transactTimeoutUser = findProperty("transactTimeout[User]");
        
        appServerConnectionString = findProperty("connectionString[AppServer]");
        appServerByConnectionString = findProperty("appServer[ISTRING[100]]");
        isStartedAppServer = findProperty("isStarted[AppServer]");
        
        useKeystore = findProperty("useKeystore[AppServer]");
        keystorePassword = findProperty("keystorePassword[AppServer]");
        keyPassword = findProperty("keyPassword[AppServer]");
        keystore = findProperty("keystore[AppServer]");
        privateKey = findProperty("privateKey[AppServer]");
        chain = findProperty("chain[AppServer]");
        privateKeyPassword = findProperty("privateKeyPassword[AppServer]");
    }
}
