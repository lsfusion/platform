package paas.properties;

import paas.PaasBusinessLogics;
import paas.PaasLogicsModule;
import platform.server.classes.ValueClass;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.UserActionProperty;

import java.sql.SQLException;

public class RefreshStatusActionProperty extends UserActionProperty {

    public RefreshStatusActionProperty(String sID, String caption, PaasLogicsModule paasLM) {
        super(sID, caption, new ValueClass[]{paasLM.project});
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        ((PaasBusinessLogics)context.getBL()).refreshConfigurationStatuses(context.getSingleKeyValue());

        context.emitExceptionIfNotInFormSession();

        context.getFormInstance().refreshData();
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.design.setIconPath("refresh.png");
    }
}
