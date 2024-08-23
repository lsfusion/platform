package lsfusion.server.logics.classes.data.utils.image.opencv;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class InitializeOpenCVAction extends InternalAction {
    public InitializeOpenCVAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            java.io.File tmpFolder = net.sourceforge.tess4j.util.LoadLibs.extractTessResources("win32-x86-64");
            lsfusion.base.SystemUtils.setLibraryPath(tmpFolder.getPath(), "java.library.path");
        } catch (NoClassDefFoundError e) {
            ServerLoggers.systemLogger.error("Failed to initialize tess4j. Add tess4j jar to classpath.");
        }
    }
}
