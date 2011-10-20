package paas.properties;

import org.apache.log4j.Logger;
import paas.PaasBusinessLogics;
import paas.PaasLogicsModule;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.DataObject;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class StartConfigurationActionProperty extends ActionProperty {
    private final static Logger logger = Logger.getLogger(StartConfigurationActionProperty.class);

    private PaasBusinessLogics paas;
    private PaasLogicsModule paasLM;

    public StartConfigurationActionProperty(PaasBusinessLogics paas, String sID, String caption) {
        super(sID, caption, new ValueClass[]{paas.paasLM.configuration});
        this.paasLM = paas.paasLM;
        this.paas = paas;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        DataObject confObj = context.getSingleKeyValue();

        try {
            paas.appManager.executeScriptedBL(context.getSession(), confObj);
        } catch (Exception e) {
            logger.warn("Ошибка при попытке запустить приложение: ", e);

            context.getActions().add(new MessageClientAction(e.getMessage(), "Ошибка!"));
        }
    }

    @Override
    public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
        super.proceedDefaultDesign(view, entity);
        view.get(entity).design.setIconPath("start.png");
    }
}
