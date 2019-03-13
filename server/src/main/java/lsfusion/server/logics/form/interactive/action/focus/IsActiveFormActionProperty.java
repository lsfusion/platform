package lsfusion.server.logics.form.interactive.action.focus;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.action.SystemExplicitActionProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;

import java.sql.SQLException;

public class IsActiveFormActionProperty extends SystemExplicitActionProperty {

    private LCP<?> isActiveFormProperty;
    private FormEntity requestedForm;

    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getChangeProps(isActiveFormProperty.property);
    }

    public IsActiveFormActionProperty(LocalizedString caption, FormEntity form, LCP isActiveFormProperty) {
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