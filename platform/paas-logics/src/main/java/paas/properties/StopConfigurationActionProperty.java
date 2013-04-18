package paas.properties;

import org.apache.log4j.Logger;
import paas.PaasBusinessLogics;
import paas.PaasLogicsModule;
import paas.manager.server.AppManager;
import platform.interop.action.MessageClientAction;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

import static platform.base.BaseUtils.isRedundantString;

public class StopConfigurationActionProperty extends ScriptingActionProperty {
    private final static Logger logger = Logger.getLogger(StopConfigurationActionProperty.class);

    public StopConfigurationActionProperty(PaasLogicsModule paasLM) {
        super(paasLM, paasLM.configuration);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        DataObject confObj = context.getSingleKeyValue();

        PaasBusinessLogics paas = (PaasBusinessLogics)context.getBL();

        String exportName = (String) paas.paasLM.configurationExportName.read(context, confObj);
        if (isRedundantString(exportName)) {
            context.delayUserInterfaction(new MessageClientAction("Имя для экспорта не задано.", "Ошибка!"));
            return;
        }

        try {
            AppManager appManager = context.getLogicsInstance().getCustomObject(AppManager.class);
            appManager.stopApplication(exportName);
        } catch (Exception e) {
            logger.warn("Ошибка при попытке остановить приложение: ", e);
            paas.changeConfigurationStatus(confObj, "stopped");
        }
    }
}
