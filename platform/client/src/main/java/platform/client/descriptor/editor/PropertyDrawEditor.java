package platform.client.descriptor.editor;

import platform.client.ListGroupObjectEditor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.PropertyObjectDescriptor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.increment.editor.*;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PropertyDrawEditor extends GroupElementEditor {
    private final PropertyDrawDescriptor descriptor;

    public PropertyDrawEditor(final GroupObjectDescriptor groupObject, final PropertyDrawDescriptor descriptor, final FormDescriptor form) {
        super(groupObject);

        this.descriptor = descriptor;

        add(new TitledPanel("Стат. заголовок", new IncrementTextEditor(descriptor, "caption")));

        add(Box.createRigidArea(new Dimension(5, 5)));
        
        add(new TitledPanel("Реализация", new PropertyObjectEditor(descriptor, "propertyObject", form, groupObject)));

        add(Box.createRigidArea(new Dimension(5, 5)));

        add(new TitledPanel("Группа объектов", new JComboBox(new IncrementSingleListSelectionModel(descriptor, "toDraw") {
            public List<?> getList() {
                PropertyObjectDescriptor propertyObject = descriptor.getPropertyObject();
                return propertyObject != null
                        ? propertyObject.getGroupObjects(form.groupObjects)
                        : new ArrayList();
            }

            public void fillListDependencies() {
                IncrementDependency.add(descriptor, "propertyObject", this);
                IncrementDependency.add(form, "groupObjects", this);
            }
        })));

        add(Box.createRigidArea(new Dimension(5, 5)));

        // columnGroupObjects из списка mapping'ов (полных) !!! без toDraw
        add(new TitledPanel("Группы в колонки", new IncrementMultipleListEditor(new IncrementMultipleListSelectionModel(descriptor, "columnGroupObjects") {
            public List<?> getList() {
                return descriptor.getUpGroupObjects(form.groupObjects);
            }

            public void fillListDependencies() {
                IncrementDependency.add(descriptor, "propertyObject", this);
                IncrementDependency.add(descriptor, "toDraw", this);
                IncrementDependency.add(form, "groupObjects", this);
            }
        })));

        add(Box.createRigidArea(new Dimension(5, 5)));

        // propertyCaption из списка columnGroupObjects (+objects без toDraw)
        add(new TitledPanel("Динам. заголовок", new IncrementDialogEditor(descriptor, "propertyCaption") {
            protected Object dialogValue(Object currentValue) {
                return new ListGroupObjectEditor(descriptor.getColumnGroupObjects()).getPropertyObject();
            }
        }));

        add(Box.createRigidArea(new Dimension(5, 5)));

        add(new TitledPanel(null, new IncrementCheckBox("Должно быть последним", descriptor, "shouldBeLast")));

        add(Box.createRigidArea(new Dimension(5, 5)));

        add(new TitledPanel("Тип просмотра", new JComboBox(new IncrementSingleListSelectionModel(descriptor, "forceViewType") {
            public List<?>  getList(){
                return ClassViewType.typeNameList();
            }
        })));

        add(new ComponentEditor("Компонетные свойства", descriptor.client));

        add(Box.createRigidArea(new Dimension(5, 5)));
    }

    @Override
    public boolean validateEditor() {
        if (descriptor.getPropertyObject() == null) {
            JOptionPane.showMessageDialog(this, "Выберите реализацию!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }
}
