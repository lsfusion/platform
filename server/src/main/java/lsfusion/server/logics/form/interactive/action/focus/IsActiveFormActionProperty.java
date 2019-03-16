package lsfusion.server.logics.form.interactive.action.focus;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class IsActiveFormActionProperty extends SystemExplicitAction {

    private LP<?> isActiveFormProperty;
    private FormEntity requestedForm;

    public ImMap<Property, Boolean> aspectChangeExtProps() {
        return getChangeProps(isActiveFormProperty.property);
    }

    public IsActiveFormActionProperty(LocalizedString caption, FormEntity form, LP isActiveFormProperty) {
        super(caption);
        this.requestedForm = form;
        this.isActiveFormProperty = isActiveFormProperty;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance activeFormInstance = context.getFormInstance(true, false);
        FormEntity activeForm = activeFormInstance == null ? null : activeFormInstance.entity;
        Boolean isActive = activeForm != null && requestedForm != null && activeForm.equals(requestedForm);
        isActiveFormProperty.change(isActive, context);
    }
}