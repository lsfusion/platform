package lsfusion.client.descriptor.editor.filters;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.editor.PropertyObjectEditor;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.filter.CompareFilterDescriptor;
import lsfusion.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import lsfusion.interop.Compare;

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
