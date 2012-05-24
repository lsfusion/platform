package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.server.classes.ValueClass;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.CustomActionProperty;

import java.awt.*;
import java.sql.SQLException;

public class ApplyActionProperty extends CustomActionProperty {
    public final static Dimension BUTTON_SIZE = new Dimension(25, 20);

    public ApplyActionProperty() {
        super("formApplyAction", ApiResourceBundle.getString("form.layout.apply"), new ValueClass[0]);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formApply(context.getActions());
    }
}
