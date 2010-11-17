package platform.client.descriptor.editor;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.TreeGroupDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementMultipleListEditor;
import platform.client.descriptor.increment.editor.IncrementMultipleListSelectionModel;

import javax.swing.*;
import java.util.List;

public class TreeGroupEditor extends JPanel implements NodeEditor {

    public TreeGroupEditor(final TreeGroupDescriptor treeGroup, final FormDescriptor form) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JList groupsEditor = new IncrementMultipleListEditor(new IncrementMultipleListSelectionModel(treeGroup, "groups") {
            public List<?> getList() {
                return form.groupObjects;
            }

            @Override
            public void fillListDependencies() {
                form.addDependency(form, "groupObjects", this);
            }
        });
        groupsEditor.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        add(new TitledPanel("Группы", groupsEditor));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
