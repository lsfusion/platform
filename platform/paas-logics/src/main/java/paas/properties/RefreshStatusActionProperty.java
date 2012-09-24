package paas.properties;

import paas.PaasBusinessLogics;
import platform.server.classes.ValueClass;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.logics.property.actions.UserActionProperty;

import java.sql.SQLException;

public class RefreshStatusActionProperty extends UserActionProperty {

    private PaasBusinessLogics paas;

    public RefreshStatusActionProperty(PaasBusinessLogics paas, String sID, String caption) {
        super(sID, caption, new ValueClass[]{paas.paasLM.project});
        this.paas = paas;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        paas.refreshConfigurationStatuses(context.getSingleKeyValue());

        context.emitExceptionIfNotInFormSession();

        context.getFormInstance().refreshData();
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.design.setIconPath("refresh.png");
    }
}
