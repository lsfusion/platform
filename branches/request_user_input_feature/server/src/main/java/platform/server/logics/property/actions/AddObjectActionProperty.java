package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.KeyStrokes;
import platform.server.classes.*;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.form.view.panellocation.ToolbarPanelLocationView;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.*;

public class AddObjectActionProperty extends CustomReadValueActionProperty {

    // barcode != null, автоматически заполнять поле barcode значением префикс + 0000 + id
    private CalcProperty barcode;
    private CalcProperty barcodePrefix;

    // quantity = true значит, что первым в интерфейсе идет количество объектов, которые нужно добавить
    private boolean quantity;

    // обозначает класс объекта, который нужно добавить
    private CustomClass valueClass;

    // автоматически заполнить указанные свойства из входов этого свойства
    private List<CalcProperty> properties;

    private CalcProperty propertyValue;
    private DataClass dataClass;

    @Override
    public Set<CalcProperty> getChangeProps() {
        return IsClassProperty.getParentProps(valueClass);
    }

    public AddObjectActionProperty(String sID, CustomClass valueClass) {
        this(sID, null, null, false, valueClass, null, null, null);
    }

    public AddObjectActionProperty(String sID, CustomClass valueClass, CalcProperty propertyValue, DataClass dataClass) {
        this(sID, null, null, false, valueClass, null, propertyValue, dataClass);
    }

    private static ValueClass[] getValueClassList(boolean quantity, List<CalcProperty> properties) {
        List<ValueClass> result = new ArrayList<ValueClass>();
        if (quantity)
            result.add(IntegerClass.instance);
        if (properties != null)
            for (CalcProperty property : properties) {
                result.add(property.getValueClass());
            }
        return result.toArray(new ValueClass[result.size()]);
    }

    private AddObjectActionProperty(String sID, CalcProperty barcode, CalcProperty barcodePrefix, boolean quantity, CustomClass valueClass, List<CalcProperty> properties, CalcProperty propertyValue, DataClass dataClass) {
        this(sID, ServerResourceBundle.getString("logics.add"), barcode, barcodePrefix, quantity, valueClass, properties, propertyValue, dataClass);
    }
    public AddObjectActionProperty(String sID, String caption, CalcProperty barcode, CalcProperty barcodePrefix, boolean quantity, CustomClass valueClass, List<CalcProperty> properties, CalcProperty propertyValue, DataClass dataClass) {
        super(sID, caption, getValueClassList(quantity, properties)); // сам класс не передаем, поскольку это свойство "глобальное"

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

    protected DataClass getReadType(ExecutionContext context) {
        if(dataClass!=null)
            return dataClass;
        if(valueClass.hasChildren())
            return valueClass.getActionClass(valueClass);
        return null;
    }

    protected void executeRead(ExecutionContext<ClassPropertyInterface> context, Object userValue) throws SQLException {
        Integer quantityAdd = 1;
        // пока привязываемся к тому, что interfaces будет выдавать все в правильном порядке
        if (quantity) {
            quantityAdd = (Integer) context.getKeyObject(interfaces.iterator().next());
        }

        ArrayList<byte[]> values = null;
        if (dataClass instanceof FileActionClass) {
            FileActionClass clazz = (FileActionClass) dataClass;
            values = clazz.getFiles(userValue);
            quantityAdd = values.size();
        }

        for (int k = 0; k < quantityAdd; k++) {
            DataObject object = context.addObject((ConcreteCustomClass)(valueClass.hasChildren()?context.getSession().baseClass.findClassID((Integer) userValue):valueClass));
            if (barcode != null) {

                String prefix = null;
                if (barcodePrefix != null)
                    prefix = (String) barcodePrefix.read(context);
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

                ((CalcProperty)barcode).change(Collections.singletonMap(BaseUtils.single(barcode.interfaces), object), context, barcode12 + checkDigit);
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
                    CalcProperty property = properties.get(i++);
                    ((CalcProperty)property).change(Collections.singletonMap(BaseUtils.single(property.interfaces), object), context, context.getKeyObject(classInterface));
                }
            }

            if (propertyValue != null) {
                ((CalcProperty)propertyValue).change(Collections.singletonMap(BaseUtils.single(propertyValue.interfaces), object), context, values.get(k));
            }
        }
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity<?> form) {
        super.proceedDefaultDraw(entity, form);
        entity.shouldBeLast = true;
        entity.forceViewType = ClassViewType.PANEL;

        entity.toDraw = form.getObject(valueClass).groupTo;
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.editKey = KeyStrokes.getAddActionPropertyKeyStroke();
        propertyView.design.setIconPath("add.png");
        propertyView.showEditKey = false;
        propertyView.setPanelLocation(new ToolbarPanelLocationView());
    }

}
