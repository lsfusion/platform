package platform.client.descriptor.view;

import platform.client.ClientTree;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.nodes.EditingTreeNode;
import platform.client.descriptor.nodes.FormNode;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;

public class FormDescriptorView extends JPanel {

    FormDescriptor form;

    ClientTree tree;
    TreeModel model;

    JPanel view;

    final RemoteDescriptorInterface remote;

    public FormDescriptorView(RemoteDescriptorInterface remote) {

        this.remote = remote;
        
        setLayout(new BorderLayout());

        view = new JPanel();

        tree = new ClientTree();

        JScrollPane pane = new JScrollPane(tree);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pane, view);
        splitPane.setResizeWeight(0.3);

        add(splitPane, BorderLayout.CENTER);
    }

    public void setModel(FormDescriptor form) {
        this.form = form;
        update();
    }

    private void update() {
        FormNode rootNode = new FormNode(form);
        rootNode.addSubTreeAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = tree.getSelectionNode();
                if (node instanceof EditingTreeNode) {
                    view.removeAll();
                    view.add(((EditingTreeNode) node).createEditor(form, remote));
                    view.validate();
                }
            }
        });

        model = new DefaultTreeModel(rootNode);
        tree.setModel(model);        
    }
}
