package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

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
        FormEntity<?> activeForm = context.getFormInstance() == null ? null : context.getFormInstance().entity;
        Boolean isActive = activeForm != null && requestedForm != null && activeForm.equals(requestedForm);
        isActiveFormProperty.change(isActive, context);
    }
}