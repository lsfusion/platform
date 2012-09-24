package paas.properties;

import org.apache.log4j.Logger;
import paas.PaasBusinessLogics;
import paas.PaasLogicsModule;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.logics.property.actions.UserActionProperty;

import java.sql.SQLException;

public class StartConfigurationActionProperty extends UserActionProperty {
    private final static Logger logger = Logger.getLogger(StartConfigurationActionProperty.class);

    private PaasBusinessLogics paas;
    private PaasLogicsModule paasLM;

    public StartConfigurationActionProperty(PaasBusinessLogics paas, String sID, String caption) {
        super(sID, caption, new ValueClass[]{paas.paasLM.configuration});
        this.paasLM = paas.paasLM;
        this.paas = paas;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        DataObject confObj = context.getSingleKeyValue();

        try {
            paas.appManager.executeScriptedBL(context.getSession(), confObj);
        } catch (Exception e) {
            logger.warn("Ошибка при попытке запустить приложение: ", e);

            context.delayUserInterfaction(new MessageClientAction(e.getMessage(), "Ошибка!"));
        }
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.design.setIconPath("start.png");
    }
}
