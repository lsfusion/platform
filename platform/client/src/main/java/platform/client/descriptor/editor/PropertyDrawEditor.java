package platform.client.descriptor.editor;

import platform.client.descriptor.*;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.editor.base.AbstractComboBoxModel;
import platform.client.descriptor.editor.base.IncrementComboBoxModel;
import platform.client.descriptor.editor.base.IncrementListModel;
import platform.interop.serialization.RemoteDescriptorInterface;
import platform.base.BaseUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;

public class PropertyDrawEditor extends GroupElementEditor {

    public PropertyDrawEditor(final GroupObjectDescriptor groupObject, final PropertyDrawDescriptor descriptor, final FormDescriptor form, final RemoteDescriptorInterface remote) {
        super(groupObject);

        final JComboBox objectEditor = new JComboBox(new IncrementComboBoxModel(descriptor, "propertyObject") {
            public List<?> getList() {
                return form.getProperties(groupObject, remote);
            }
            public void fillListDependencies() {
                IncrementDependency.add(form, "groups", this);
            }
        });
        add(objectEditor);

        // все не обязательно но желательно
        if(groupObject!=null) {
            descriptor.toDraw = groupObject;
        } else {
            final JComboBox toDrawEditor = new JComboBox(new IncrementComboBoxModel(descriptor, "toDraw") {
                public List<?> getList() {
                    return descriptor.propertyObject.getGroupObjects(form.groups);
                }
                public void fillListDependencies() {
                    IncrementDependency.add(descriptor, "propertyObject", this);
                    IncrementDependency.add(form, "groups", this);
                }
            });
            add(toDrawEditor);
        }

        // columnGroupObjects из списка mapping'ов (полных) !!! без toDraw
        final JList columnGroupList = new JList(new IncrementListModel(){
            public List<?> getList() {
                return BaseUtils.removeList(descriptor.propertyObject.getGroupObjects(form.groups), Collections.singleton(descriptor.toDraw));
            }

            public void fillListDependencies() {
                IncrementDependency.add(descriptor, "propertyObject", this);
                IncrementDependency.add(descriptor, "toDraw", this);
                IncrementDependency.add(form, "groups", this);
            }
        });
        for(GroupObjectDescriptor columnGroup : descriptor.columnGroupObjects)
            columnGroupList.setSelectedValue(columnGroup, false);
        columnGroupList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                descriptor.columnGroupObjects = new ArrayList<GroupObjectDescriptor>();
                for(Object selectedValue : columnGroupList.getSelectedValues())
                    descriptor.columnGroupObjects.add((GroupObjectDescriptor) selectedValue);
            }
        });

        // propertyCaption из списка columnGroupObjects (+objects без toDraw)
        final JComboBox captionEditor = new JComboBox(new AbstractComboBoxModel() {
            public int getSize() {
                return FormDescriptor.getProperties(descriptor.columnGroupObjects, null, remote).size();
            }
            public Object getElementAt(int index) {
                return FormDescriptor.getProperties(descriptor.columnGroupObjects, null, remote).get(index);
            }
        });
        captionEditor.setSelectedItem(descriptor.propertyCaption);
        captionEditor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                descriptor.propertyCaption = (PropertyObjectDescriptor)captionEditor.getSelectedItem();
            }
        });
        add(objectEditor);
    }

}
