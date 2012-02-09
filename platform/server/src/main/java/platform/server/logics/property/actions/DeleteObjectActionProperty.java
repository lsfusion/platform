package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.interop.KeyStrokes;
import platform.server.classes.BaseClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class DeleteObjectActionProperty extends CustomActionProperty {

    public DeleteObjectActionProperty(BaseClass baseClass) {
        super("delete", ServerResourceBundle.getString("logics.property.actions.delete"), new ValueClass[]{baseClass});
    }

    public String getCode() {
        return "delete";
    }

    public void execute(ExecutionContext context) throws SQLException {
        context.getFormInstance().changeClass((CustomObjectInstance) context.getSingleObjectInstance(), context.getSingleKeyValue(), -1);
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity form) {
        super.proceedDefaultDraw(entity, form);
        entity.shouldBeLast = true;
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.editKey = KeyStrokes.getDeleteActionPropertyKeyStroke();
        propertyView.design.setIconPath("delete.png");
        propertyView.showEditKey = false;
    }
}
