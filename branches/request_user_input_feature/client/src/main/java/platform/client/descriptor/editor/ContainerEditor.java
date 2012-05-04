package platform.client.descriptor.editor;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.editor.base.NorthBoxPanel;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementCheckBox;
import platform.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import platform.client.descriptor.increment.editor.IncrementTextEditor;
import platform.client.logics.ClientContainer;
import platform.interop.form.layout.ContainerType;

import javax.swing.*;
import java.awt.*;
import java.awt.List;
import java.util.*;


public class ContainerEditor extends JTabbedPane implements NodeEditor {

    public ContainerEditor(ClientContainer descriptor) {

        addTab(ClientResourceBundle.getString("descriptor.editor.common"), new NorthBoxPanel(new TitledPanel(ClientResourceBundle.getString("descriptor.editor.common.title"), new IncrementTextEditor(descriptor, "title")),
                new TitledPanel(ClientResourceBundle.getString("descriptor.editor.common.description"), new IncrementTextEditor(descriptor, "description")),
                new TitledPanel(ClientResourceBundle.getString("descriptor.editor.common.identificator"), new IncrementTextEditor(descriptor, "sID"))));

        addTab(ClientResourceBundle.getString("descriptor.editor.display.display"), new NorthBoxPanel(
                new TitledPanel(ClientResourceBundle.getString("descriptor.editor.display.container.type"), new JComboBox(new IncrementSingleListSelectionModel(descriptor, "stringType") {
                    @Override
                    public java.util.List<?> getSingleList() {
                        return ContainerType.getTypeNamesList();
                    }
                })),
                new TitledPanel(null, new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.display.default.component"), descriptor, "defaultComponent")),
                new SizesEditor(descriptor),
                new ComponentDesignEditor(ClientResourceBundle.getString("descriptor.editor.display.design"), descriptor.design)));
        
        addTab(ClientResourceBundle.getString("descriptor.editor.arrangement"), new NorthBoxPanel(new ComponentIntersectsEditor(ClientResourceBundle.getString("descriptor.editor.mutual.arrangement.of.the.components"), descriptor, "intersects"),
                new ContainerConstraintsEditor(descriptor.constraints)));

    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
