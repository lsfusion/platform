package platform.client.descriptor.view;

import platform.client.ClientTree;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.increment.IncrementView;
import platform.client.descriptor.nodes.actions.AddingTreeNode;
import platform.client.descriptor.nodes.actions.EditingTreeNode;
import platform.client.descriptor.nodes.FormNode;
import platform.client.descriptor.nodes.PlainTextNode;
import platform.client.descriptor.nodes.actions.FilterAction;
import platform.client.navigator.ClientNavigator;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class FormDescriptorView extends JPanel implements IncrementView {

    FormDescriptor form;

    ClientTree tree;
    DefaultTreeModel model;

    JPanel view;

    final RemoteDescriptorInterface remote;
    private JButton previewBtn;
    private JButton saveBtn;
    private ClientNavigator navigator;

    public FormDescriptorView(ClientNavigator iNavigator, RemoteDescriptorInterface remote) {
        this.navigator = iNavigator;

        this.remote = remote;

        setLayout(new BorderLayout());

        view = new JPanel();

        tree = new ClientTree();

        tree.setDropMode(DropMode.ON);
        tree.setDragEnabled(true);

        previewBtn = new JButton("Предпросмотр формы");
        previewBtn.setEnabled(false);
        previewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreviewDialog dlg = new PreviewDialog(navigator, form);
                dlg.setBounds(SwingUtilities.windowForComponent(FormDescriptorView.this).getBounds());
                dlg.setVisible(true);
            }
        });

        saveBtn = new JButton("Сохранить форму");
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    navigator.remoteNavigator.saveForm(form.getID(), FormDescriptor.serialize(form));
                } catch (IOException ioe) {
                    throw new RuntimeException("Не могу сохранить форму.", ioe);
                }
            }
        });

        JPanel commandPanel = new JPanel();
        commandPanel.add(previewBtn);
        commandPanel.add(saveBtn);

        JPanel formTreePanel = new JPanel();
        formTreePanel.setLayout(new BorderLayout());

        formTreePanel.add(new JScrollPane(tree), BorderLayout.CENTER);
        formTreePanel.add(commandPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formTreePanel, view);
        splitPane.setResizeWeight(0.3);

        add(splitPane, BorderLayout.CENTER);

        IncrementDependency.add("groupObjects", this);
        IncrementDependency.add("propertyDraws", this);
        IncrementDependency.add("children", this);
        IncrementDependency.add("caption", this);
        IncrementDependency.add("propertyObject", this);
        IncrementDependency.add("toDraw", this);
        IncrementDependency.add(this, "form", this);
    }

    public void setModel(FormDescriptor form) {
        this.form = form;

        previewBtn.setEnabled(form != null);
        saveBtn.setEnabled(form != null);

        IncrementDependency.update(this, "form");
    }

    public void update(Object updateObject, String updateField) {

        TreeNode refreshNode;
        if(form!=null) {
            FormNode rootNode = new FormNode(form);
            rootNode.addSubTreeAction(
                    new FilterAction("Редактировать") {
                        public boolean isApplicable(TreePath path) {
                            return path != null && path.getLastPathComponent() instanceof EditingTreeNode;
                        }

                        public void actionPerformed(ActionEvent e) {
                            DefaultMutableTreeNode node = tree.getSelectionNode();
                            if (node instanceof EditingTreeNode) {
                                view.removeAll();
                                view.add(((EditingTreeNode) node).createEditor(form, remote));
                                view.validate();
                                view.updateUI();
                            }
                        }
                    });

            rootNode.addSubTreeAction(
                    new FilterAction("Добавить") {
                        public boolean isApplicable(TreePath path) {
                            return path != null && path.getLastPathComponent() instanceof AddingTreeNode;
                        }

                        public void actionPerformed(ActionEvent e) {
                            DefaultMutableTreeNode node = tree.getSelectionNode();
                            if (node instanceof AddingTreeNode) {
                                AddingTreeNode addingNode = (AddingTreeNode) node;
                                addingNode.addNewElement(tree.getSelectionPath());
                            }
                        }
                    });

            refreshNode = rootNode;
        } else
            refreshNode = new PlainTextNode("Форма не выбрана");

        model = new DefaultTreeModel(refreshNode);
        tree.setModelPreservingState(model);
    }
}
