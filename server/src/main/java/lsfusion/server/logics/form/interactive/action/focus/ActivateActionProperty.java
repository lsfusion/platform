package lsfusion.server.logics.form.interactive.action.focus;

import lsfusion.interop.action.ActivateFormClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class ActivateActionProperty extends SystemExplicitAction {

    private FormEntity requestedForm;
    private ComponentView requestedTab;

    public ActivateActionProperty(LocalizedString caption, FormEntity form, ComponentView requestedTab) {
        super(caption);
        this.requestedForm = form;
        this.requestedTab = requestedTab;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        if(requestedForm != null) {
            if (requestedTab == null) {
                //activate form
                context.delayUserInteraction(new ActivateFormClientAction(requestedForm.getCanonicalName()));
            } else {
                //activate tab
                FormInstance formInstance = context.getFormInstance(false, true);
                formInstance.activateTab(requestedTab);
            }
        }

    }
}