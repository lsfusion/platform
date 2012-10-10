package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.server.classes.ColorClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.DataSession;

import java.awt.*;
import java.sql.SQLException;

public class FormApplyActionProperty extends FormToolbarActionProperty {
    public FormApplyActionProperty() {
        super("formApply", ApiResourceBundle.getString("form.layout.apply"), DataSession.isDataChanged,
              new CalcProperty[] {FormEntity.manageSession, FormEntity.isReadOnly}, new boolean[] {false, true});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getFormInstance().formApply();
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> propertyDraw, FormEntity<?> form) {
        super.proceedDefaultDraw(propertyDraw, form);

        propertyDraw.propertyBackground = form.addPropertyObject(new LCP(
                DerivedProperty.createAnd(DerivedProperty.<ClassPropertyInterface>createStatic(Color.green, ColorClass.instance), DataSession.isDataChanged.getImplement()).property));
    }
}
