package paas.properties;

import org.apache.log4j.Logger;
import paas.PaasBusinessLogics;
import paas.PaasLogicsModule;
import paas.manager.server.AppManager;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.UserActionProperty;

import java.sql.SQLException;

public class StopConfigurationActionProperty extends UserActionProperty {
    private final static Logger logger = Logger.getLogger(StopConfigurationActionProperty.class);

    public StopConfigurationActionProperty(String sID, String caption, PaasLogicsModule paasLM) {
        super(sID, caption, new ValueClass[]{paasLM.configuration});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        DataObject confObj = context.getSingleKeyValue();

        PaasBusinessLogics paas = (PaasBusinessLogics)context.getBL();

        Integer port = (Integer) paas.paasLM.configurationPort.read(context, confObj);
        if (port == null) {
            context.delayUserInterfaction(new MessageClientAction("Порт не задан.", "Ошибка!"));
            return;
        }

        try {
            AppManager appManager = context.getLogicsInstance().getCustomObject(AppManager.class);
            appManager.stopApplication(port);
        } catch (Exception e) {
            logger.warn("Ошибка при попытке остановить приложение: ", e);
            paas.changeConfigurationStatus(confObj, "stopped");
        }
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.design.setIconPath("stop.png");
    }
}
