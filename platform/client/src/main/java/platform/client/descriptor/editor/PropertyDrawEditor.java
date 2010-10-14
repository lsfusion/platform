package platform.client.descriptor.editor;

import platform.client.descriptor.*;
import platform.client.descriptor.editor.base.NamedContainer;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.increment.editor.IncrementComboBoxModel;
import platform.client.descriptor.increment.editor.IncrementListEditor;
import platform.client.descriptor.increment.editor.IncrementListModel;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;
import java.util.*;
import java.util.List;
import java.awt.*;

public class PropertyDrawEditor extends GroupElementEditor {

    public PropertyDrawEditor(final GroupObjectDescriptor groupObject, final PropertyDrawDescriptor descriptor, final FormDescriptor form, final RemoteDescriptorInterface remote) {
        super(groupObject);

        add(new NamedContainer("Реализация", false, new JComboBox(new IncrementComboBoxModel(descriptor, "propertyObject") {
            public List<?> getList() {
                return form.getProperties(groupObject, remote);
            }
            public void fillListDependencies() {
                IncrementDependency.add(form, "groups", this);
            }
        })));

        add(Box.createRigidArea(new Dimension(5,5)));

        // все не обязательно но желательно
        if(groupObject==null) {
            add(new NamedContainer("Группа объектов", false, new JComboBox(new IncrementComboBoxModel(descriptor, "toDraw") {
                public List<?> getList() {
                    return descriptor.getPropertyObject().getGroupObjects(form.groups);
                }
                public void fillListDependencies() {
                    IncrementDependency.add(descriptor, "propertyObject", this);
                    IncrementDependency.add(form, "groups", this);
                }
            })));

            add(Box.createRigidArea(new Dimension(5,5)));
        }

        // columnGroupObjects из списка mapping'ов (полных) !!! без toDraw
        add(new NamedContainer("Группы в колонки", true, new IncrementListEditor(descriptor, "columnGroupObjects", new IncrementListModel(){
            public List<?> getList() {
                return descriptor.getUpGroupObjects(form.groups);
            }

            public void fillListDependencies() {
                IncrementDependency.add(descriptor, "propertyObject", this);
                IncrementDependency.add(form, "groups", this);
                IncrementDependency.add(descriptor, "toDraw", this);
            }
        })));

        add(Box.createRigidArea(new Dimension(5,5)));
        
        // propertyCaption из списка columnGroupObjects (+objects без toDraw)
        add(new NamedContainer("Заголовок", false, new JComboBox(new IncrementComboBoxModel(descriptor, "propertyCaption") {
            public List<?> getList() {
                return FormDescriptor.getProperties(descriptor.getColumnGroupObjects(), null, remote);
            }
            public void fillListDependencies() {
                IncrementDependency.add(descriptor, "columnGroupObjects", this);
            }
        })));
    }

}
