package platform.client.descriptor.editor;

import platform.client.descriptor.*;
import platform.client.descriptor.editor.base.NamedContainer;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import platform.client.descriptor.increment.editor.IncrementMultipleListSelectionModel;
import platform.client.descriptor.increment.editor.IncrementTextEditor;
import platform.client.descriptor.increment.editor.IncrementMultipleListEditor;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;

public class PropertyDrawEditor extends GroupElementEditor {
    private final PropertyDrawDescriptor descriptor;

    public PropertyDrawEditor(final GroupObjectDescriptor groupObject, final PropertyDrawDescriptor descriptor, final FormDescriptor form, final RemoteDescriptorInterface remote) {
        super(groupObject);

        this.descriptor = descriptor;

        add(new NamedContainer("Стат. заголовок", false, new IncrementTextEditor(descriptor, "caption")));

        add(new NamedContainer("Реализация", false, new JComboBox(new IncrementSingleListSelectionModel(descriptor, "propertyObject") {
            public List<?> getList() {
                return form.getProperties(groupObject);
            }
            public void fillListDependencies() {
                IncrementDependency.add(form, "groupObjects", this);
            }
        })));

        add(Box.createRigidArea(new Dimension(5,5)));

        // все не обязательно но желательно
        if(groupObject==null) {
            add(new NamedContainer("Группа объектов", false, new JComboBox(new IncrementSingleListSelectionModel(descriptor, "toDraw") {
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

            add(Box.createRigidArea(new Dimension(5,5)));
        }

        // columnGroupObjects из списка mapping'ов (полных) !!! без toDraw
        add(new NamedContainer("Группы в колонки", true, new IncrementMultipleListEditor(new IncrementMultipleListSelectionModel(descriptor, "columnGroupObjects") {
            public List<?> getList() {
                return descriptor.getUpGroupObjects(form.groupObjects);
            }

            public void fillListDependencies() {
                IncrementDependency.add(descriptor, "propertyObject", this);
                IncrementDependency.add(descriptor, "toDraw", this);
                IncrementDependency.add(form, "groupObjects", this);
            }
        })));

        add(Box.createRigidArea(new Dimension(5,5)));
        
        // propertyCaption из списка columnGroupObjects (+objects без toDraw)
        add(new NamedContainer("Динам. заголовок", false, new JComboBox(new IncrementSingleListSelectionModel(descriptor, "propertyCaption") {
            public List<?> getList() {
                return FormDescriptor.getProperties(descriptor.getColumnGroupObjects(), null, remote);
            }
            public void fillListDependencies() {
                IncrementDependency.add(descriptor, "columnGroupObjects", this);
            }
        })));
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
