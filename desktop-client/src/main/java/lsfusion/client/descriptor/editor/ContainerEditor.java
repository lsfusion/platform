package lsfusion.client.descriptor.editor;

import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.editor.base.NorthBoxPanel;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.IncrementCheckBox;
import lsfusion.client.descriptor.increment.editor.IncrementSingleListSelectionModel;
import lsfusion.client.descriptor.increment.editor.IncrementTextEditor;
import lsfusion.client.logics.ClientContainer;
import lsfusion.interop.form.layout.ContainerType;

import javax.swing.*;
import java.util.Arrays;

import static lsfusion.client.ClientResourceBundle.getString;


public class ContainerEditor extends JTabbedPane implements NodeEditor {

    public ContainerEditor(ClientContainer container) {

        addTab(getString("descriptor.editor.common"), new NorthBoxPanel(new TitledPanel(getString("descriptor.editor.common.caption"), new IncrementTextEditor(container, "rawCaption")),
                                                                                             new TitledPanel(getString("descriptor.editor.common.description"), new IncrementTextEditor(container, "description")),
                                                                                             new TitledPanel(getString("descriptor.editor.common.identificator"), new IncrementTextEditor(container, "sID"))));

        addTab(getString("descriptor.editor.display.display"), new NorthBoxPanel(
                new TitledPanel(getString("descriptor.editor.display.container.type"), new JComboBox(new IncrementSingleListSelectionModel(container, "type") {
                    @Override
                    public java.util.List<?> getSingleList() {
                        return Arrays.asList(ContainerType.values());
                    }
                })),
                new TitledPanel(null, new IncrementCheckBox(getString("descriptor.editor.display.default.component"), container, "defaultComponent")),
                new SizesEditor(container),
                new ComponentDesignEditor(getString("descriptor.editor.display.design"), container.design)));

        addTab(getString("descriptor.editor.arrangement"),
               new NorthBoxPanel(
                       new ContainerConstraintsEditor(container)
               ));

    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
