package paas.properties;

import org.apache.log4j.Logger;
import paas.PaasBusinessLogics;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.DataObject;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class StopConfigurationActionProperty extends ActionProperty {
    private final static Logger logger = Logger.getLogger(StopConfigurationActionProperty.class);

    private PaasBusinessLogics paas;

    public StopConfigurationActionProperty(PaasBusinessLogics paas, String sID, String caption) {
        super(sID, caption, new ValueClass[]{paas.paasLM.configuration});
        this.paas = paas;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        DataObject confObj = context.getSingleKeyValue();

        Integer port = (Integer) paas.paasLM.configurationPort.read(context.getSession(), confObj);
        if (port == null) {
            context.getActions().add(new MessageClientAction("Порт не задан.", "Ошибка!"));
            return;
        }

        try {
            paas.appManager.stopApplication(port);
        } catch (Exception e) {
            logger.warn("Ошибка при попытке остановить приложение: ", e);
            paas.changeConfigurationStatus(confObj, "stopped");
        }
    }

    @Override
    public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
        super.proceedDefaultDesign(view, entity);
        view.get(entity).design.setIconPath("stop.png");
    }
}
