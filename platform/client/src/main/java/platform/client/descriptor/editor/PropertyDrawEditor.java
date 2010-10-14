package platform.client.descriptor.editor;

import platform.client.descriptor.*;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.editor.base.IncrementComboBoxModel;
import platform.client.descriptor.editor.base.IncrementListModel;
import platform.client.descriptor.editor.base.IncrementListEditor;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;
import java.util.*;

public class PropertyDrawEditor extends GroupElementEditor {

    public PropertyDrawEditor(final GroupObjectDescriptor groupObject, final PropertyDrawDescriptor descriptor, final FormDescriptor form, final RemoteDescriptorInterface remote) {
        super(groupObject);

        add(new JComboBox(new IncrementComboBoxModel(descriptor, "propertyObject") {
            public List<?> getList() {
                return form.getProperties(groupObject, remote);
            }
            public void fillListDependencies() {
                IncrementDependency.add(form, "groups", this);
            }
        }));

        // все не обязательно но желательно
        if(groupObject!=null) {
            add(new JComboBox(new IncrementComboBoxModel(descriptor, "toDraw") {
                public List<?> getList() {
                    return descriptor.getPropertyObject().getGroupObjects(form.groups);
                }
                public void fillListDependencies() {
                    IncrementDependency.add(descriptor, "propertyObject", this);
                    IncrementDependency.add(form, "groups", this);
                }
            }));
        }

        // columnGroupObjects из списка mapping'ов (полных) !!! без toDraw
        add(new IncrementListEditor(descriptor, "columnGroupObjects", new IncrementListModel(){
            public List<?> getList() {
                return descriptor.getUpGroupObjects(form.groups);
            }

            public void fillListDependencies() {
                IncrementDependency.add(descriptor, "propertyObject", this);
                IncrementDependency.add(form, "groups", this);
                IncrementDependency.add(descriptor, "toDraw", this);
            }
        }));

        // propertyCaption из списка columnGroupObjects (+objects без toDraw)
        add(new JComboBox(new IncrementComboBoxModel(descriptor, "propertyCaption") {
            public List<?> getList() {
                return FormDescriptor.getProperties(descriptor.getColumnGroupObjects(), null, remote);
            }
            public void fillListDependencies() {
                IncrementDependency.add(descriptor, "columnGroupObjects", this);
            }
        }));
    }

}
