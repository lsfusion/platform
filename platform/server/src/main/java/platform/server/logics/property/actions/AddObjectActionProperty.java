package platform.server.logics.property.actions;

import platform.interop.action.ClientAction;
import platform.interop.ClassViewType;
import platform.server.classes.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.view.DefaultFormView;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.FormEntity;

import javax.swing.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

public class AddObjectActionProperty extends ActionProperty {

    private CustomClass valueClass;

    public AddObjectActionProperty(String sID, CustomClass valueClass) {
        super(sID, "Добавить (" + valueClass + ")", new ValueClass[] {}); // сам класс не передаем, поскольку это свойство "глобальное"

        this.valueClass = valueClass;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        FormInstance<?> form = (FormInstance<?>)executeForm.form;
        if (valueClass.hasChildren())
            form.addObject((ConcreteCustomClass)form.getCustomClass((Integer)value.getValue()));
        else
            form.addObject((ConcreteCustomClass)valueClass);
    }

    @Override
    protected DataClass getValueClass() {
        if (valueClass.hasChildren())
            return valueClass.getActionClass(valueClass);
        else
            return super.getValueClass();
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity form) {
        super.proceedDefaultDraw(entity, form);
        entity.shouldBeLast = true;
        entity.forceViewType = ClassViewType.PANEL;
    }
    @Override
    public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
        super.proceedDefaultDesign(view, entity);
        view.get(entity).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_DOWN_MASK);
    }

}
