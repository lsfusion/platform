package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.server.classes.ColorClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.DataSession;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;

import static platform.server.logics.property.derived.DerivedProperty.createAnd;
import static platform.server.logics.property.derived.DerivedProperty.createStatic;

public class ApplyActionProperty extends FormToolbarActionProperty {
    private final BusinessLogics BL;

    public ApplyActionProperty(BusinessLogics BL) {
        super("apply", ApiResourceBundle.getString("form.layout.apply"), DataSession.isDataChanged, FormEntity.isNewSession);

        this.BL = BL;
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.apply(BL);
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> propertyDraw, FormEntity<?> form) {
        super.proceedDefaultDraw(propertyDraw, form);

        ArrayList<PropertyInterface> interfaces = new ArrayList<PropertyInterface>();
        propertyDraw.propertyBackground = form.addPropertyObject(
                new LCP(createAnd(interfaces, createStatic(Color.green, ColorClass.instance), DataSession.isDataChanged.getImplement(interfaces)).property));
    }
}
