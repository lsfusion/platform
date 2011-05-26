package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.interop.KeyStrokes;
import platform.interop.action.ClientAction;
import platform.server.classes.BaseClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import javax.swing.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DeleteObjectActionProperty extends ActionProperty {

    public DeleteObjectActionProperty(String sID, BaseClass baseClass) {
        super(sID, "Удалить", new ValueClass[]{baseClass});
    }

    public String getCode() {
        return "delete";
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
        ((FormInstance<?>) executeForm.form).changeClass((CustomObjectInstance) BaseUtils.singleValue(mapObjects), BaseUtils.singleValue(keys), -1);
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity form) {
        super.proceedDefaultDraw(entity, form);
        entity.shouldBeLast = true;
    }

    @Override
    public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
        super.proceedDefaultDesign(view, entity);
        view.get(entity).editKey = KeyStrokes.getDeleteActionPropertyKeyStroke();
        view.get(entity).design.image = new ImageIcon(AddObjectActionProperty.class.getResource("/images/delete.png"));
        view.get(entity).showEditKey = false;
    }
}
