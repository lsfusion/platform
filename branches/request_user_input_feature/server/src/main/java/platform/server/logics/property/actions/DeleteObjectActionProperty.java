package platform.server.logics.property.actions;

import platform.interop.KeyStrokes;
import platform.server.classes.BaseClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DeleteObjectActionProperty extends CustomActionProperty {

    public DeleteObjectActionProperty(BaseClass baseClass) {
        super("delete", ServerResourceBundle.getString("logics.property.actions.delete"), new ValueClass[]{baseClass});
    }

    public String getCode() {
        return "delete";
    }

    public void executeCustom(ExecutionContext context) throws SQLException {
        // после удаления выбираем соседний объект
        DataObject nearObject = null;
        PropertyObjectInterfaceInstance objectInstance = context.getSingleObjectInstance();
        if (objectInstance != null) {
            List<Map<ObjectInstance, DataObject>> keys = ((CustomObjectInstance) objectInstance).groupTo.keys.keyList();
            for (Map<ObjectInstance, DataObject> key : keys) {
                if (key.values().contains(context.getSingleKeyValue()) && nearObject == null) {
                    int index = keys.indexOf(key);
                    if (keys.size() == 1)
                        continue;
                    index = index == keys.size() - 1 ? index - 1 : index + 1;
                    nearObject = keys.get(index).get(objectInstance);
                }
            }
        }

        context.changeClass(objectInstance, context.getSingleKeyValue(), -1);

        if (nearObject != null)
            ((CustomObjectInstance) objectInstance).groupTo.addSeek(objectInstance, nearObject, false);
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity<?> form) {
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
