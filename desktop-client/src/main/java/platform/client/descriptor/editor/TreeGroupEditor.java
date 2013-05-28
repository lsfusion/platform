package platform.client.descriptor.editor;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.TreeGroupDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementCheckBox;
import platform.client.descriptor.increment.editor.IncrementMultipleListEditor;
import platform.client.descriptor.increment.editor.IncrementMultipleListSelectionModel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TreeGroupEditor extends JPanel implements NodeEditor {

    public TreeGroupEditor(final TreeGroupDescriptor treeGroup, final FormDescriptor form) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JList groupsEditor = new IncrementMultipleListEditor(new IncrementMultipleListSelectionModel(treeGroup, "groups") {
            public List<?> getList() {
                ArrayList<GroupObjectDescriptor> result = new ArrayList<GroupObjectDescriptor>();
                for (GroupObjectDescriptor group : form.groupObjects) {
                    TreeGroupDescriptor parent = group.getParent();
                    if (parent == treeGroup || parent == null) {
                        result.add(group);
                    }
                }
                return result;
            }

            @Override
            public void fillListDependencies() {
                form.addDependency(form, "groupObjects", this);
            }
        });
        groupsEditor.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        add(new TitledPanel(ClientResourceBundle.getString("descriptor.editor.group.groups"), groupsEditor));

        JPanel plainTreeModePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        plainTreeModePanel.add(new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.group.show.in.plain.tree.mode"), treeGroup, "plainTreeMode"));
        add(plainTreeModePanel);
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
