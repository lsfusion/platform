package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class IsFormActiveActionProperty extends SystemExplicitActionProperty {

    private LCP<?> isFormActiveProperty;
    private FormEntity requestedForm;

    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getChangeProps(isFormActiveProperty.property);
    }

    public IsFormActiveActionProperty(String caption, FormEntity form, LCP isFormActiveProperty) {
        super(caption);
        this.requestedForm = form;
        this.isFormActiveProperty = isFormActiveProperty;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormEntity<?> activeForm = context.getFormInstance() == null ? null : context.getFormInstance().entity;
        Boolean isActive = activeForm != null && requestedForm != null && activeForm.equals(requestedForm);
        isFormActiveProperty.change(isActive, context);
    }
}