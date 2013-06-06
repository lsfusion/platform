package lsfusion.client.descriptor.editor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.base.NorthBoxPanel;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.IncrementCheckBox;
import lsfusion.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import lsfusion.client.descriptor.increment.editor.IncrementTextEditor;
import lsfusion.client.logics.ClientContainer;
import lsfusion.interop.form.layout.ContainerType;

import javax.swing.*;


public class ContainerEditor extends JTabbedPane implements NodeEditor {

    public ContainerEditor(ClientContainer descriptor) {

        addTab(ClientResourceBundle.getString("descriptor.editor.common"), new NorthBoxPanel(new TitledPanel(ClientResourceBundle.getString("descriptor.editor.common.caption"), new IncrementTextEditor(descriptor, "rawCaption")),
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
