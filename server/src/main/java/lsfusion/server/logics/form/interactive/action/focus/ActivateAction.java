package lsfusion.server.logics.form.interactive.action.focus;

import lsfusion.interop.action.ActivateFormClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.action.FormAddress;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class ActivateAction extends SystemExplicitAction {

    // ACTIVATE TAB: the form the tab is on, and the tab
    private final FormEntity requestedForm;
    private final ComponentView requestedTab;
    // ACTIVATE FORM: the address, of which the form is one part and may itself be absent
    private final FormAddress address;

    public ActivateAction(LocalizedString caption, FormEntity form, ComponentView requestedTab) {
        super(caption);
        this.requestedForm = form;
        this.requestedTab = requestedTab;
        this.address = null;
    }

    public ActivateAction(LocalizedString caption, FormAddress address) {
        super(caption);
        this.requestedForm = null;
        this.requestedTab = null;
        this.address = address;
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        if(address != null) {
            context.delayUserInteraction(new ActivateFormClientAction(address.formCanonicalName, address.formId, address.windowCanonicalName));
            return;
        }

        // the tab is activated only on the form that is running the action: a tab of a form the user is not in is
        // not something to select
        FormInstance activeFormInstance = context.getFormInstance(false, true);
        FormEntity activeForm = activeFormInstance == null ? null : activeFormInstance.entity;
        if(activeForm != null && activeForm.equals(requestedForm))
            activeFormInstance.activateTab(activeFormInstance.instanceFactory.getExInstance(requestedTab).entity);
    }
}