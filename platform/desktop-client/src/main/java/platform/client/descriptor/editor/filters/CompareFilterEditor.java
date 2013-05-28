package platform.client.descriptor.editor.filters;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.PropertyObjectEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.filter.CompareFilterDescriptor;
import platform.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import platform.interop.Compare;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class CompareFilterEditor extends PropertyFilterEditor {

    public CompareFilterEditor(GroupObjectDescriptor group, CompareFilterDescriptor descriptor, FormDescriptor form) {
        super(group, descriptor, form);

        add(Box.createRigidArea(new Dimension(5, 5)));

        add(new TitledPanel(ClientResourceBundle.getString("descriptor.filter.operation"), new JComboBox(new IncrementSingleListSelectionModel(descriptor, "compare") {
            public List<?> getSingleList() {
                return Arrays.asList(Compare.values());
            }
        })));

        add(Box.createRigidArea(new Dimension(5, 5)));

        add(new TitledPanel(ClientResourceBundle.getString("descriptor.filter.compare.with"), new PropertyObjectEditor(descriptor, "value", form, group)));
    }
}
