package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.KeyStrokes;
import platform.interop.action.ClientAction;
import platform.server.classes.*;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import javax.swing.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class AddObjectActionProperty extends ActionProperty {

    // barcode != null, автоматически заполнять поле barcode значением префикс + 0000 + id
    private Property barcode;
    private Property barcodePrefix;

    // quantity = true значит, что первым в интерфейсе идет количество объектов, которые нужно добавить
    private boolean quantity;

    // обозначает класс объекта, который нужно добавить
    private CustomClass valueClass;

    // автоматически заполнить указанные свойства из входов этого свойства
    private List<Property> properties;

    private Property propertyValue;
    private DataClass dataClass;

    public AddObjectActionProperty(String sID, CustomClass valueClass) {
        this(sID, null, null, false, valueClass, null, null, null);
    }

    public AddObjectActionProperty(String sID, CustomClass valueClass, Property propertyValue, DataClass dataClass) {
        this(sID, null, null, false, valueClass, null, propertyValue, dataClass);
    }

    private static ValueClass[] getValueClassList(boolean quantity, List<Property> properties) {
        List<ValueClass> result = new ArrayList<ValueClass>();
        if (quantity)
            result.add(IntegerClass.instance);
        if (properties != null)
            for (Property property : properties) {
                result.add(property.getCommonClasses().value);
            }
        return result.toArray(new ValueClass[result.size()]);
    }

    public AddObjectActionProperty(String sID, Property barcode, Property barcodePrefix, boolean quantity, CustomClass valueClass, List<Property> properties, Property propertyValue, DataClass dataClass) {
        super(sID, "Добавить (" + valueClass + ")", getValueClassList(quantity, properties)); // сам класс не передаем, поскольку это свойство "глобальное"

        this.barcode = barcode;
        this.barcodePrefix = barcodePrefix;
        this.quantity = quantity;
        this.valueClass = valueClass;
        this.properties = properties;
        this.propertyValue = propertyValue;
        this.dataClass = dataClass;
    }

    @Override
    public String getCode() {
        return "getAddObjectAction(" + valueClass.getSID() + ")";
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
        FormInstance<?> form = (FormInstance<?>) executeForm.form;

        Integer quantityAdd = 1;
        // пока привязываемся к тому, что interfaces будет выдавать все в правильном порядке
        if (quantity) {
            quantityAdd = (Integer) keys.get(interfaces.iterator().next()).getValue();
        }

        ArrayList<byte[]> values = null;
        if (dataClass instanceof FileActionClass) {
            FileActionClass clazz = (FileActionClass) dataClass;
            values = clazz.getFiles(value.getValue());
            quantityAdd = values.size();
        }

        for (int k = 0; k < quantityAdd; k++) {
            DataObject object;
            if (valueClass.hasChildren())
                object = form.addObject((ConcreteCustomClass) form.getCustomClass((Integer) value.getValue()));
            else
                object = form.addObject((ConcreteCustomClass) valueClass);

            if (barcode != null) {

                String prefix = null;
                if (barcodePrefix != null)
                    prefix = (String) barcodePrefix.read(session, modifier);
                if (prefix == null) prefix = "";
                prefix = prefix.trim();

                String id = object.getValue().toString();

                String barcode12 = prefix + BaseUtils.replicate('0', Math.max(12 - prefix.length() - id.length(), 0)) + id;
                int evenSum = 0;
                int oddSum = 0;
                for (int i = 0; i < barcode12.length(); i++) {
                    if ((i + 1) % 2 == 0)
                        evenSum += Integer.parseInt(barcode12.substring(i, i + 1));
                    else
                        oddSum += Integer.parseInt(barcode12.substring(i, i + 1));
                }
                int checkDigit = (evenSum * 3 + oddSum) % 10 == 0 ? 0 : 10 - (evenSum * 3 + oddSum) % 10;

                barcode.execute(Collections.singletonMap(BaseUtils.single(barcode.interfaces), object), session,
                        barcode12 + checkDigit, modifier);
            }

            // меняем все свойства на значения входов
            if (properties != null) {
                // пока считаем, что в interfaces параметры идут в том же порядке, что и в properties
                int i = 0;
                boolean first = true;
                for (ClassPropertyInterface classInterface : interfaces) {
                    if (quantity && first) {
                        first = false;
                        continue;
                    }
                    Property property = properties.get(i++);
                    property.execute(Collections.singletonMap(BaseUtils.single(property.interfaces), object),
                            session, keys.get(classInterface).getValue(), modifier);
                }
            }

            if (propertyValue != null) {
                propertyValue.execute(Collections.singletonMap(BaseUtils.single(propertyValue.interfaces), object),
                        session, values.get(k), modifier);
            }
        }
    }

    @Override
    public DataClass getValueClass() {
        if (dataClass != null) {
            return dataClass;
        }
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
        view.get(entity).editKey = KeyStrokes.getAddActionPropertyKeyStroke();
        view.get(entity).design.setImage(new ImageIcon(AddObjectActionProperty.class.getResource("/images/add.png")));
        view.get(entity).showEditKey = false;
        view.get(entity).drawToToolbar = true;
    }

}
