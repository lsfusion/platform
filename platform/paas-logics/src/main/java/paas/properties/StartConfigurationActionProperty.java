package paas.properties;

import org.apache.log4j.Logger;
import paas.PaasLogicsModule;
import paas.manager.server.AppManager;
import platform.interop.action.MessageClientAction;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class StartConfigurationActionProperty extends ScriptingActionProperty {
    private final static Logger logger = Logger.getLogger(StartConfigurationActionProperty.class);

    public StartConfigurationActionProperty(PaasLogicsModule paasLM) {
        super(paasLM, paasLM.configuration);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        DataObject confObj = context.getSingleDataKeyValue();

        try {
            AppManager appManager = context.getLogicsInstance().getCustomObject(AppManager.class);
            appManager.executeConfiguration(context.getSession(), confObj);
        } catch (Exception e) {
            logger.warn("Ошибка при попытке запустить приложение: ", e);

            context.delayUserInterfaction(new MessageClientAction(e.getMessage(), "Ошибка!"));
        }
    }
}
