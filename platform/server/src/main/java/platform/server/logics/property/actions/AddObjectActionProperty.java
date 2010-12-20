package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.action.ClientAction;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.session.DataSession;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.*;

public class AddObjectActionProperty extends ActionProperty {

    private CustomClass valueClass;
    private List<Property> properties;

    public AddObjectActionProperty(String sID, CustomClass valueClass) {
        this(sID, valueClass, null);
    }

    private static ValueClass[] getValueClassList(List<Property> properties) {
        List<ValueClass> result = new ArrayList<ValueClass>();
        if (properties != null)
            for (Property property : properties) {
                result.add(property.getCommonClasses().value);
            }
        return result.toArray(new ValueClass[0]);
    }

    public AddObjectActionProperty(String sID, CustomClass valueClass, List<Property> properties) {
        super(sID, "Добавить (" + valueClass + ")", getValueClassList(properties)); // сам класс не передаем, поскольку это свойство "глобальное"

        this.valueClass = valueClass;
        this.properties = properties;
    }

    @Override
    public String getCode() {
        return "getAddObjectAction(" + valueClass.getSID() + ")";
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        FormInstance<?> form = (FormInstance<?>)executeForm.form;
        DataSession session = form.session;
        DataObject object;
        if (valueClass.hasChildren())
            object = form.addObject((ConcreteCustomClass)form.getCustomClass((Integer)value.getValue()));
        else
            object = form.addObject((ConcreteCustomClass)valueClass);

        // меняем все свойства на значения входов
        if (properties != null) {
            // пока считаем, что в interfaces параметры идут в том же порядке, что и в properties
            int i = 0;
            for (ClassPropertyInterface classInterface : interfaces) {
                Property property = properties.get(i++);
                property.execute(Collections.singletonMap(BaseUtils.single(property.interfaces), object),
                                 session, keys.get(classInterface).getValue(), form);
            }
        }
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
