package platform.server.logics.property.actions.form;

import platform.base.ApiResourceBundle;
import platform.server.classes.ColorClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.session.DataSession;

import java.awt.*;
import java.sql.SQLException;

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

        propertyDraw.propertyBackground = form.addPropertyObject(
                BL.LM.addJProp(BL.LM.and1, BL.LM.addCProp(ColorClass.instance, Color.green), new LCP(DataSession.isDataChanged)));
    }
}
