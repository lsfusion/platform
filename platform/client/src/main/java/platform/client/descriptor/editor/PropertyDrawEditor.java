package platform.client.descriptor.editor;

import platform.client.descriptor.*;
import platform.interop.serialization.RemoteDescriptorInterface;
import platform.base.BaseUtils;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;

public class PropertyDrawEditor extends GroupElementEditor {

    public PropertyDrawEditor(final GroupObjectDescriptor groupObject, final PropertyDrawDescriptor descriptor, final FormDescriptor form, final RemoteDescriptorInterface remote) {
        super(groupObject);

        final JComboBox objectEditor = new JComboBox(new AbstractComboBoxModel() {
            public int getSize() {
                return form.getProperties(groupObject, remote).size();
            }
            public Object getElementAt(int index) {
                return form.getProperties(groupObject, remote).get(index);
            }
        });
        objectEditor.setSelectedItem(descriptor.propertyObject);
        objectEditor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                descriptor.propertyObject = (PropertyObjectDescriptor)objectEditor.getSelectedItem();
            }
        });
        add(objectEditor);

        // все не обязательно но желательно
        if(groupObject!=null) {
            descriptor.toDraw = groupObject;
        } else {
            final JComboBox toDrawEditor = new JComboBox(new AbstractComboBoxModel() {
                public int getSize() {
                    return descriptor.propertyObject.getGroupObjects(form.groups).size();
                }
                public Object getElementAt(int index) {
                    return descriptor.propertyObject.getGroupObjects(form.groups).get(index);
                }
            });
            toDrawEditor.setSelectedItem(descriptor.toDraw);
        }

        // columnGroupObjects из списка mapping'ов (полных) !!! без toDraw
        final JList columnGroupList = new JList(new AbstractListModel(){
            public int getSize() {
                return BaseUtils.removeList(descriptor.propertyObject.getGroupObjects(form.groups), Collections.singleton(descriptor.toDraw)).size();
            }

            public Object getElementAt(int index) {
                return BaseUtils.removeList(descriptor.propertyObject.getGroupObjects(form.groups), Collections.singleton(descriptor.toDraw)).get(index);
            }
        });
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

    private abstract class AbstractComboBoxModel extends AbstractListModel implements ComboBoxModel {
        Object selectedObject = null;

        public void setSelectedItem(Object anItem) {
            if (!BaseUtils.nullEquals(selectedObject, anItem)) {
                selectedObject = anItem;
                fireContentsChanged(this, -1, -1);
            }
        }

        public Object getSelectedItem() {
            return selectedObject;
        }
    }
}
